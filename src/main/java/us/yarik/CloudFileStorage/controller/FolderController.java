package us.yarik.CloudFileStorage.controller;

import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import us.yarik.CloudFileStorage.dto.FolderDTO;
import us.yarik.CloudFileStorage.model.File;
import us.yarik.CloudFileStorage.service.FileService;
import us.yarik.CloudFileStorage.service.MinioService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
//directory
//TODO delete folder(dont delete files and folders on that folder)
//TODO put folder to folder
//TODO download folder?

//TODO delete folder(red button)
//TODO put to folder on folder(folder page)

//TODO file finder
@RestController
@RequiredArgsConstructor
public class FolderController {
    private final FileService fileService;
    private final MinioService minioService;

    @PostMapping("/create-folder")
    public ResponseEntity<File> createFolder(@RequestBody FolderDTO folderDTO) {
        File folder = File.builder()
                .fileName(folderDTO.getFolderName())
                .isFolder(true)
                .fileSize(0L)
                .uploadDate(LocalDateTime.now())
                .parentId(folderDTO.getParentId())
                .owner(folderDTO.getOwner())
                .build();

        fileService.uploadFile(folder);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/files/folder/{parentId}")
    public List<File> allFilesFromFolder(@PathVariable("parentId") String parentId) {
        return fileService.findByParentId(parentId);
    }

//    @GetMapping("/download/{fileId}/{folder}")
//    public ResponseEntity<byte[]> downloadFileInFolder(@PathVariable String fileId, @PathVariable String folder){
//        File file = fileService.findById(fileId);
//        ByteArrayResource fileDownload = minioService.downloadFile(file.getOwner() + "-" + file.getFileName() + "-" + file.getUuid() + "-" + folder);
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
//                        URLEncoder.encode(file.getFileName().replace(" ", "_"), StandardCharsets.UTF_8))
//                .contentType(MediaType.valueOf(file.getFileType()))
//                .body(fileDownload.getByteArray());
//    }
//
//    @DeleteMapping("/delete/{fileId}/{folder}")
//    public void deleteFileInFolder(@PathVariable("fileId") String fileId, @PathVariable("folder") String folder) throws ServerException,
//            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
//            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        File file = fileService.findById(fileId);
//        minioService.deleteFile(file.getOwner() + "-" + file.getFileName() + "-" + file.getUuid() + "-" + folder);
//        fileService.deleteFile(file);
//    }
//
//    @PostMapping("/copy/{fileId}")
//    public void copyFileInFolder(@PathVariable("fileId") String fileId, @PathVariable("folder") String folder) throws IOException, ServerException,
//            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
//            InvalidResponseException, XmlParserException, InternalException {
//        File file = fileService.findById(fileId);
//        String uuid = UUID.randomUUID().toString();
//        String sanitizedFileName = file.getFileName().replaceAll("[<>:\"/\\|?*]", "_");
//        Path path = Paths.get("bucket" + java.io.File.separator + file.getOwner() + "-" + sanitizedFileName + "-" + uuid + "-" + folder);
//
//        File fileCopy = new File();
//        fileCopy.setFileName(file.getFileName());
//        fileCopy.setFileType(file.getFileType());
//        fileCopy.setFileSize(file.getFileSize());
//        fileCopy.setUploadDate(LocalDateTime.now());
//        fileCopy.setOwner(file.getOwner());
//        fileCopy.setMinioPath(path.toString());
//        fileCopy.setUuid(uuid);
//        fileService.uploadFile(fileCopy);
//        InputStream inputStream = minioService.getFile(file.getOwner(), file.getFileName(), file.getUuid());
//        minioService.addFile(file.getOwner(), file.getFileName(), inputStream , file.getFileType(), uuid);
//    }
//
//    @PostMapping("/rename/{fileId}")
//    public ResponseEntity<String> renameFileInFolder(@PathVariable("fileId") String fileId,
//                                                     @PathVariable("folder") String folder,
//                                                     @RequestBody Map<String, String> request) throws
//            IOException {
//        String newFileName = request.get("newFileName");
//        String oldFileName = fileService.findById(fileId).getFileName();
//        fileService.updateFileName(fileService.findById(fileId), newFileName);
//        minioService.renameFileInFolder(oldFileName, newFileName,
//                fileService.findById(fileId).getOwner(), fileService.findById(fileId).getUuid(), folder );
//        return ResponseEntity.ok("File rename seccessfully!");
//    }
//
//    @PostMapping("/api/{fileId}/{folder}")
//    public void putFileToFolder(@PathVariable("fileId") String fileId, @PathVariable("folder") String folder){
//        File file = fileService.findById(fileId);
//        String fullFileName = file.getOwner() + "-" + file.getFileName() + "-" + file.getUuid();
//        minioService.uploadFileToFolder(fullFileName, folder);
//        //fileService.setFolder(fileId, folder);
//    }
//
//    @PostMapping("/delete-file-from-folder/{fileId}/{folder}")
//    public void deleteFileFromFolder(@PathVariable("fildeId") String fileId,
//                                     @PathVariable("folder") String folder){
//        File file = fileService.findById(fileId);
//        String fullFileName = file.getOwner() + "-" + file.getFileName() + "-" + file.getUuid();
//        //fileService.deleteFileFromFolder(file);
//        minioService.deleteFileFromFolder(fullFileName, folder);
//
//    }

    @PostMapping("/add-file/{owner}/{parentId}")
    public ResponseEntity<String> addFileToFolder(@PathVariable("owner") String owner,
                                                  @PathVariable("parentId") String parentId,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            String uuid = UUID.randomUUID().toString();
            String fileName = file.getOriginalFilename();
            String sanitizedFileName = fileName.replaceAll("[<>:\"/\\|?*]", "_");
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();
            Path path = Paths.get("bucket" + java.io.File.separator + owner + "-" + sanitizedFileName + "-" + uuid + "-" + parentId);

            File newFile = File.builder()
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploadDate(LocalDateTime.now())
                    .owner(owner)
                    .minioPath(path.toString())
                    .uuid(uuid)
                    .parentId(parentId)
                    .build();

            fileService.uploadFile(newFile);
            minioService.addFile(owner, fileName, inputStream, contentType, uuid, parentId);

            return ResponseEntity.ok("File uploaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error " + e.getMessage());
        }
    }
    @GetMapping("/check-folder-name/{folderName}")
    public ResponseEntity<?> checkFolderName(@PathVariable("folderName") String folderName,
                                             @RequestParam String owner){
        File file = fileService.findByOwnerAndFileName(owner,  folderName);
        boolean isNameUnique = (file == null);
        System.out.println(isNameUnique);
        return ResponseEntity.ok().body(Map.of("isNameUnique", isNameUnique));
    }
    @PostMapping("/copy-folder/{fileId}")
    public ResponseEntity<String> copyFolder(@PathVariable("fileId") String fileId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InvalidResponseException, InternalException {
        File file = fileService.findById(fileId);
        List<File> filesOnFolder = fileService.findByOwnerAndFileNameList(file.getOwner(), file.getFileName());
        System.out.println(filesOnFolder.toString());
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedDate = myFormatObj.format(now);
        FolderDTO folderDTO = new FolderDTO( file.getFileName() + " " + formattedDate, null, file.getOwner());
        createFolder(folderDTO);
        for(File files: filesOnFolder){
            if(!filesOnFolder.isEmpty()) {
                String uuid = UUID.randomUUID().toString();
                String sanitizedFileName = files.getFileName().replaceAll("[<>:\"/\\|?*]", "_");
                Path path = Paths.get("bucket" + java.io.File.separator + files.getOwner() + "-" + sanitizedFileName + "-" + uuid);

                File fileCopy = new File();
                fileCopy.setFileName(files.getFileName());
                fileCopy.setFileType(files.getFileType());
                fileCopy.setFileSize(files.getFileSize());
                fileCopy.setUploadDate(LocalDateTime.now());
                fileCopy.setOwner(files.getOwner());
                fileCopy.setMinioPath(path.toString());
                fileCopy.setUuid(uuid);
                fileCopy.setParentId(folderDTO.getFolderName());
                fileService.uploadFile(fileCopy);
                InputStream inputStream = minioService.getFileFromFolder(files.getOwner(), files.getFileName(), files.getUuid(), files.getParentId());
                minioService.addFile(files.getOwner(), fileCopy.getFileName(), inputStream, files.getFileType(), uuid, fileCopy.getParentId());
            }
        }
        return ResponseEntity.ok("Folder copied!");
    }

    @PostMapping("/copy-file-on-folder/{fileId}")
    public void copyFile(@PathVariable("fileId") String fileId) throws IOException, ServerException,
            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        File file = fileService.findById(fileId);
        String uuid = UUID.randomUUID().toString();
        String sanitizedFileName = file.getFileName().replaceAll("[<>:\"/\\|?*]", "_");
        Path path = Paths.get("bucket" + java.io.File.separator + file.getOwner() + "-" + sanitizedFileName + "-" + uuid);

        File fileCopy = new File();
        fileCopy.setFileName(file.getFileName());
        fileCopy.setFileType(file.getFileType());
        fileCopy.setFileSize(file.getFileSize());
        fileCopy.setUploadDate(LocalDateTime.now());
        fileCopy.setOwner(file.getOwner());
        fileCopy.setMinioPath(path.toString());
        fileCopy.setUuid(uuid);
        fileCopy.setParentId(file.getParentId());
        fileService.uploadFile(fileCopy);
        InputStream inputStream = minioService.getFileFromFolder(file.getOwner(), file.getFileName(), file.getUuid(), file.getParentId());
        minioService.addFile(file.getOwner(), file.getFileName(), inputStream, file.getFileType(), uuid, fileCopy.getParentId());
    }

    @PostMapping("/rename-on-folder/{fileId}")
    public ResponseEntity<String> renameFile(@PathVariable("fileId") String fileId,
                                             @RequestBody Map<String, String> request) throws
            IOException {
        String newFileName = request.get("newFileName");
        String oldFileName = fileService.findById(fileId).getFileName();
        fileService.updateFileName(fileService.findById(fileId), newFileName);
        minioService.renameFileInFolder(oldFileName, newFileName,
                fileService.findById(fileId).getOwner(), fileService.findById(fileId).getUuid(), fileService.findById(fileId).getParentId());
        return ResponseEntity.ok("File rename successfully!");
    }

    @GetMapping("/download-from-folder/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        File file = fileService.findById(fileId);
        ByteArrayResource fileDownload = minioService.downloadFile(file.getOwner() + "-" + file.getFileName() + "-" + file.getUuid() + "-" + file.getParentId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(file.getFileName().replace(" ", "_"), StandardCharsets.UTF_8))
                .contentType(MediaType.valueOf(file.getFileType()))
                .body(fileDownload.getByteArray());
    }

}
