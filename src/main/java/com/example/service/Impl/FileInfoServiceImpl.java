package com.example.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.constants.Constants;
import com.example.entity.dto.Result;
import com.example.entity.dto.UserDTO;
import com.example.entity.enums.*;
import com.example.entity.po.FileInfo;
import com.example.entity.po.User;
import com.example.entity.query.FileInfoQuery;
import com.example.entity.vo.FileInfoVO;
import com.example.entity.vo.FolderVO;
import com.example.entity.vo.ShareInfoVO;
import com.example.mapper.UserMapper;
import com.example.mapper.FileInfoMapper;
import com.example.service.FileInfoService;
import com.example.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.utils.RedisContants.LOGIN_TOKEN_KEY;

/**
 * 文件信息 业务接口实现
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper,FileInfo> implements FileInfoService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private FileInfoMapper fileInfoMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Async
    public Result uploadFile(FileInfoVO fileInfoVo, HttpSession session) {
        String token =session.getAttribute("token").toString();
        UserDTO userDTO = getUserDto(token);
        Long size = fileInfoVo.getFileSize() + userDTO.getUseSpace();
        if (size > userDTO.getTotalSpace()) {
            return Result.fail("可用空间不足");
        }
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("file_md5",fileInfoVo.getFileMd5());
        FileInfo empty = fileInfoMapper.selectOne(queryWrapper);
        if(empty!=null){
            //秒传
            empty.setCreateTime(new Date());
            empty.setUserId(userDTO.getId());
            save(empty);
            return Result.ok();
        }
        //分块的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileInfoVo.getFileMd5());
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (fileInfoVo.getFolderType()==1&&!chunkFileFolder.exists()) {
            boolean mkdirs = chunkFileFolder.mkdirs();
            empty.setFileName(fileInfoVo.getFileName());
            empty.setFolderType(fileInfoVo.getFolderType());
            empty.setFileCover(fileInfoVo.getFileCover());
            empty.setUserId(fileInfoVo.getUserId());
            //logger.info("创建分片文件夹:{}", mkdirs);
        }
        //写入分片
        try (
                InputStream inputStream = fileInfoVo.getFile().getInputStream();
                FileOutputStream outputStream = new FileOutputStream(chunkFileFolderPath + fileInfoVo.getChunkIndex())
        ) {
            IOUtils.copy(inputStream, outputStream);
            //logger.info("文件标识:{},chunkNumber:{}", fileInfoVo.getIdentifier(), fileInfoVo.getChunkNumber());
            //将该分片写入redis
            long rSize = saveToRedis(fileInfoVo);
            //合并分片
            if (rSize == fileInfoVo.getTotalChunks()) {
                if (mergeChunks(fileInfoVo, userDTO.getId())) {
                    return Result.fail("合并文件失败");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateUserSpace(token, userDTO, size);
        return Result.ok();
    }

    //合并分片
    private boolean mergeChunks(FileInfoVO fileInfoVO,Long userId) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileInfoVO.getFileMd5());
        String filePath = getFilePath(fileInfoVO.getFileMd5(), fileInfoVO.getFileName());
        File chunkFileFolder = new File(chunkFileFolderPath);
        File mergeFile = new File(filePath);
        File[] chunks = chunkFileFolder.listFiles();
        //排序
        Arrays.stream(chunks).sorted(Comparator.comparing(o -> Integer.valueOf(o.getName())));
        try {
            RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw");
            byte[] bytes = new byte[1024];
            for (File chunk : chunks) {
                RandomAccessFile randomAccessFileReader = new RandomAccessFile(chunk, "r");
                int len;
                while ((len = randomAccessFileReader.read(bytes)) != -1) {
                    randomAccessFileWriter.write(bytes, 0, len);
                }
                randomAccessFileReader.close();
            }
            randomAccessFileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFilePid(fileInfoVO.getFilePid());
        fileInfo.setUserId(userId);
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFileMd5(fileInfoVO.getFileMd5());
        fileInfo.setFileName(fileInfoVO.getFileName());
        fileInfo.setFilePath(fileInfoVO.getFileCover());
        save(fileInfo);
        return true;
    }

    //分片写入Redis
    private synchronized long saveToRedis(FileInfoVO fileInfoVo) {
        Set<Integer> uploaded = (Set<Integer>) stringRedisTemplate.opsForHash().get(fileInfoVo.getFileMd5(), "uploaded");
        if (uploaded == null) {
            uploaded = new HashSet(Arrays.asList(fileInfoVo.getChunkIndex()));
            HashMap<String, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("uploaded", uploaded);
            objectObjectHashMap.put("totalChunks", fileInfoVo.getTotalChunks());
            objectObjectHashMap.put("fileSize", fileInfoVo.getFileSize());
            objectObjectHashMap.put("path", getFileRelativelyPath(fileInfoVo.getFileMd5(), fileInfoVo.getFileName()));
            stringRedisTemplate.opsForHash().putAll(fileInfoVo.getFileMd5(), objectObjectHashMap);
        } else {
            uploaded.add(fileInfoVo.getChunkIndex());
            stringRedisTemplate.opsForHash().put(fileInfoVo.getFileMd5(), "uploaded", uploaded);
        }
        return uploaded.size();
    }

    //得到文件的绝对路径
    private String getFilePath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return getFileFolderPath(identifier) + identifier + ext;
    }

    //得到文件的相对路径
    private String getFileRelativelyPath(String identifier, String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        return "/" + identifier.substring(0, 1) + "/" +
                identifier.substring(1, 2) + "/" +
                identifier + "/" + identifier
                + ext;
    }


    //得到分块文件所属的目录
    private String getChunkFileFolderPath(String identifier) {
        return getFileFolderPath(identifier) + "chunks" + File.separator;
    }

    private final static String uploadFolder = "/Users/yuxiang/Downloads/kunCloud";

    //得到文件所属的目录
    private String getFileFolderPath(String identifier) {
        return uploadFolder + identifier.substring(0, 1) + File.separator +
                identifier.substring(1, 2) + File.separator +
                identifier + File.separator;
    }

    private void updateUserSpace(String token, UserDTO user, Long fileSize) {
        user.setUseSpace(user.getUseSpace() - fileSize);
        stringRedisTemplate.opsForHash().put(token,"userSpace",user.getUseSpace());
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("id",user.getId());
        //userMapper.update(user,queryWrapper);
        update().eq("id",user.getId()).update();
    }

    private void cutFile4Video(String fileId, String videoFilePath) throws Exception {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd1 = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        System.out.println(cmd1);
        String[] str1 = cmd1.split("\\s+");
        //ProcessUtils.executeCommand(cmd, false);
        ProcessBuilder pb1 = new ProcessBuilder();
        pb1.command(str1).start();
        //生成索引文件.m3u8 和切片.ts
        String cmd2 = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        System.out.println(cmd2);
        String[] str2 = cmd2.split("\\s+");
        //ProcessUtils.executeCommand(cmd, false);
        ProcessBuilder pb2 = new ProcessBuilder();
        pb2.command(str2).start();
        //删除index.ts
        new File(tsPath).delete();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rename(String fileId, String userId, String fileName) {
        QueryWrapper queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("fileId",fileId);
        FileInfo fileInfo = getOne(queryWrapper);
        if (fileInfo == null) {
            return Result.fail("文件不存在");
        }
        if (fileInfo.getFileName().equals(fileName)) {
            return Result.ok(fileInfo);
        }
        Long filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        //文件获取后缀
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        update(dbInfo, query().eq("fileId",fileId));
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            return Result.fail("文件名" + fileName + "已经存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return Result.ok(fileInfo);
    }

    private void checkFileName(Long filePid, Long userId, String fileName, Integer folderType) throws Exception {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new Exception("此目录下已存在同名文件，请修改名称");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result newFolder(FolderVO folderVO) {
        checkFileName(folderVO.getFilePid(), folderVO.getUserId(), folderVO.getFileName(), FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(redisIdWorker.nextId());
        fileInfo.setUserId(folderVO.getUserId());
        fileInfo.setFilePid(folderVO.getFilePid());
        fileInfo.setFileName(folderVO.getFileName());
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        save(fileInfo);

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(folderVO.getFilePid());
        fileInfoQuery.setUserId(folderVO.getUserId());
        fileInfoQuery.setFileName(folderVO.getFileName());
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = fileInfoMapper.selectCount();
        if (count > 1) {
            throw new Exception("文件夹" + folderName + "已经存在");
        }
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(curDate);
        return Result.ok(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.USING.getFlag());
        }
        //将目录下的所有文件更新为已删除
        if (!delFilePidList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(updateInfo, userId, delFilePidList, null, FileDelFlagEnums.USING.getFlag());
        }

        //将选中的文件更新为回收站
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.USING.getFlag());
    }


    private void findAllSubFolderFileIdList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(fileIdList, userId, fileInfo.getFileId(), delFlag);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //查询所有跟目录的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = this.fileInfoMapper.selectList(query);

        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        //查询所有所选文件
        //将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null, FileDelFlagEnums.DEL.getFlag());
        }
        //将选中的文件更新为正常,且父级目录到跟目录
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.RECYCLE.getFlag());

        //将所选文件重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            //文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        if (!adminOp) {
            query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        }
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }

        //删除所选文件，子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            this.fileInfoMapper.delFileBatch(userId, delFileSubFolderFileIdList, null, adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        //删除所选文件
        this.fileInfoMapper.delFileBatch(userId, null, Arrays.asList(fileIdArray), adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());

        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        this.userMapper.updateByUserId(userInfo, userId);

        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

    }

    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }

    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }

    @Override
    public void saveShare(ShareInfoVO shareInfoVO,HttpSession httpSession) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("file_id",shareInfoVO.getFileId());
        UserDTO userDTO = getUserDto(httpSession.getAttribute("token").toString());
        FileInfo fileInfo = fileInfoMapper.selectOne(queryWrapper);
        fileInfo.setUserId(null);
        fileInfo.setFilePid(shareInfoVO.getFilePid());
        fileInfo.setLastUpdateTime(new Date());
        save(fileInfo);
        updateUserSpace(shareInfoVO.getUserId(), fileInfo.getFileSize()+);



        String[] shareFileIdArray = shareFileIds.split(",");
        //目标目录文件列表
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(cureentUserId);
        fileInfoQuery.setFilePid(myFolderId);
        List<FileInfo> currentFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        //选择的文件
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(shareUserId);
        fileInfoQuery.setFileIdArray(shareFileIdArray);
        List<FileInfo> shareFileList = this.fileInfoMapper.selectList(fileInfoQuery);
        //重命名选择的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, cureentUserId, curDate, myFolderId);
        }
        this.fileInfoMapper.insertBatch(copyFileList);

        //更新空间
        Long useSpace = this.fileInfoMapper.selectUseSpace(cureentUserId);
        User dbUserInfo = this.userMapper.selectByUserId(cureentUserId);
        if (useSpace > dbUserInfo.getTotalSpace()) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        User userInfo = new User();
        userInfo.setUseSpace(useSpace);
        this.userMapper.updateByUserId(userInfo, cureentUserId);
        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(cureentUserId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(cureentUserId, userSpaceDto);
    }

    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo fileInfo, String sourceUserId, String currentUserId, Date curDate, String newFilePid) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId = StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(sourceFileId);
            query.setUserId(sourceUserId);
            List<FileInfo> sourceFileList = this.fileInfoMapper.selectList(query);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList, item, sourceUserId, currentUserId, curDate, newFileId);
            }
        }
    }

    public UserDTO getUserDto(String token) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_TOKEN_KEY + token);
        UserDTO userDTO = BeanUtil.mapToBean(entries,UserDTO.class,false,CopyOptions.create());
        return  userDTO;
    }
}