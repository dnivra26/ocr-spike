package hello;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class FileUploadController {
    Jedis jedis;

    public FileUploadController() throws IOException {
        jedis = new Jedis("localhost");
    }

    @PostMapping("/")
    public ResponseEntity handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {

        uploadFileToGCS(file);
        String textFromOCR = runOCROnFiler(file);
        cacheOCROutput(file, textFromOCR);

        return ResponseEntity.status(HttpStatus.OK).body("imported successfully");
    }

    private void cacheOCROutput(MultipartFile file, String textFromOCR) throws IOException {

        jedis.set(file.getName(), textFromOCR);
    }

    private void uploadFileToGCS(MultipartFile file) throws IOException {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Bucket bucket = storage.get("spike-documents");
        bucket.create(file.getName(), file.getInputStream(), "text/plain");
    }

    private String runOCROnFiler(MultipartFile file) throws IOException {
        ImageAnnotatorClient imageAnnotatorClient = ImageAnnotatorClient.create();

        ByteString imgBytes = ByteString.copyFrom(file.getBytes());

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();

        BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(Arrays.asList(request));

        List<AnnotateImageResponse> responses = response.getResponsesList();
        return responses.get(0).getFullTextAnnotation().getText();

    }


}
