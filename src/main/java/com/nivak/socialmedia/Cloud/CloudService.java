package com.nivak.socialmedia.Cloud;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudService {

    private Cloudinary cloudinary;

    @Autowired
    public void CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }


    // Profile Image Upload
    public String profileImage(MultipartFile file, String imageName) throws IOException {
        String folderName = "Profile";
        return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folderName, // Specify folder name
                "public_id",imageName,
                "resource_type", "auto" // Auto-detect resource type (image/video)
        )).get("secure_url").toString();
    }

    // Profile Image Delete if ulr presents
    public void deleteProfileImage(String imageUrl) {
        try {
            String[] parts = imageUrl.split("/");
            String filename = parts[parts.length - 1];
            cloudinary.uploader().destroy(parts[7]+"/"+filename.substring(0, filename.lastIndexOf('.')), ObjectUtils.emptyMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Post Upload
    public String postUpload(MultipartFile post, String folderName, String postName) throws IOException{
        return cloudinary.uploader().upload(post.getBytes(), ObjectUtils.asMap(
            "folder", folderName,
            "public_id", postName,
            "resource_type", "auto"
        )).get("secure_url").toString();
    }


    
}
