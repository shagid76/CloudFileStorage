package us.yarik.CloudFileStorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import us.yarik.CloudFileStorage.model.File;
import us.yarik.CloudFileStorage.repository.FileRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public List<File> findAll() {
        return fileRepository.findAll();
    }

    public File uploadFile(File file) {
        return fileRepository.save(file);
    }

    public List<File> findByOwner(String owner) {
        return fileRepository.findByOwner(owner);
    }

    public void deleteFile(File file) {
        fileRepository.delete(file);
    }

    public File findByOwnerAndFileName(String owner, String fileName) {
        Optional<File> file = fileRepository.findByOwnerAndFileName(owner, fileName);
        if (file.isPresent()) {
            return file.get();
        }
        return file.orElse(null);
    }

    public void updateFileName(File file, String fileName) {
        file.setFileName(fileName);
        fileRepository.save(file);
    }
    public void deleteFilesByOwner(String owner){
        fileRepository.deleteByOwner(owner);
    }
}
