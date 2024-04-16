package com.example.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Result;
import com.example.entity.po.FileInfo;
import com.example.entity.vo.FileInfoVO;
import com.example.mapper.FileInfoMapper;
import com.example.service.FileInfoService;
import jakarta.annotation.Resource;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

@Service
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final static String uploadFolder = "F:\\upload\\";

    //检查文件是否存在，如果存在则跳过该文件的上传，如果不存在，返回需要上传的分片集合
    @Override
    public Result checkChunkExist(FileInfoVO fileInfoVo) {
        //1.检查文件是否已上传过
        //1.1)检查在磁盘中是否存在
        String fileFolderPath = getFileFolderPath(fileInfoVo.getFileMd5());
        String filePath = getFilePath(fileInfoVo.getFileMd5(), fileInfoVo.getFileName());
        File file = new File(filePath);
        boolean exists = file.exists();
        //1.2)检查Redis中是否存在,并且所有分片已经上传完成。
        Set<Integer> uploaded = (Set<Integer>) stringRedisTemplate.opsForHash().get(fileInfoVo.getFileMd5(), "uploaded");
        if (uploaded != null && uploaded.size() == fileInfoVo.getTotalChunks() && exists) {
            return Result.ok();
        }
        File fileFolder = new File(fileFolderPath);
        if (fileInfoVo.getFolderType()==1&&!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            //logger.info("准备工作,创建文件夹,fileFolderPath:{},mkdirs:{}", fileFolderPath, mkdirs);
        }
        return Result.ok(uploaded);
    }


    //上传分片
    @Override
    public void uploadChunk(FileInfoVO fileInfoVo) {
        //分块的目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileInfoVo.getFileMd5());
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()) {
            boolean mkdirs = chunkFileFolder.mkdirs();
            //logger.info("创建分片文件夹:{}", mkdirs);
        }
        //写入分片
        try (
                InputStream inputStream = fileInfoVo.getFile().getInputStream();
                FileOutputStream outputStream = new FileOutputStream(new File(chunkFileFolderPath + fileInfoVo.getChunkIndex()))
        ) {
            IOUtils.copy(inputStream, outputStream);
            //logger.info("文件标识:{},chunkNumber:{}", fileInfoVo.getIdentifier(), fileInfoVo.getChunkNumber());
            //将该分片写入redis
            long size = saveToRedis(fileInfoVo);
            //合并分片
            if (size == fileInfoVo.getTotalChunks()) {
                File mergeFile = mergeChunks(fileInfoVo.getFileMd5(), fileInfoVo.getFileName());
                if (mergeFile == null) {
                    throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, "合并文件失败");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER, e.getMessage());
        }
    }

    //合并分片
    private File mergeChunks(String identifier, String filename) {
        String chunkFileFolderPath = getChunkFileFolderPath(identifier);
        String filePath = getFilePath(identifier, filename);
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
            throw new BusinessException(BusinessErrorCode.INVALID_PARAMETER);
        }
        return mergeFile;
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

    //得到文件所属的目录
    private String getFileFolderPath(String identifier) {
        return uploadFolder + identifier.substring(0, 1) + File.separator +
                identifier.substring(1, 2) + File.separator +
                identifier + File.separator;
    }
}
