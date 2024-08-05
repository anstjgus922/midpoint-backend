package com.solucitation.midpoint_backend.domain.file.controller;

import com.solucitation.midpoint_backend.domain.community_board.entity.Image;
import com.solucitation.midpoint_backend.domain.file.service.S3Service;
import com.solucitation.midpoint_backend.domain.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * S3 파일 작업을 처리하는 REST 컨트롤러.
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class S3Controller {
    private final S3Service s3Service;
    private final MemberService memberService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3Controller(S3Service s3Service, MemberService memberService) {
        this.s3Service = s3Service;
        this.memberService = memberService;
    }

    /**
     * 파일을 S3에 업로드하는 엔드포인트.
     *
     * @param testFile 업로드할 MultipartFile
     * @return 업로드된 파일의 URL을 포함하는 ResponseEntity
     * @throws IOException 파일 업로드 중 에러 발생 시
     */
    @PostMapping(path = "/s3/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart(value = "testFile") MultipartFile testFile) throws IOException {
        String testFileName = testFile.getOriginalFilename();
        String testFileUrl = s3Service.upload("mytest", testFileName, testFile);
        log.info("testFile Url is : " + testFileUrl);
        return new ResponseEntity<>("testFile URL: " + testFileUrl, HttpStatus.OK);
    }

    /**
     * S3에서 파일을 삭제하는 엔드포인트.
     *
     * @return 삭제 상태를 포함하는 ResponseEntity
     */
    @DeleteMapping(path = "/s3/test1")
    public ResponseEntity<String> deleteFile1(@RequestParam(value = "fileUrl") String imageUrl) {
        try {
//            log.info("profileImgUrl는 by test1? " + imageUrl);
            String default_profile_image_url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, "ap-northeast-2", "profile-images/default_image.png").toLowerCase().trim();
//            log.info("default_image_url은 ?? " + default_profile_image_url);
            if (!imageUrl.equals(default_profile_image_url)){ // 기본 이미지가 아닌 경우에만 S3에서 삭제
                 log.info("기본 이미지와 삭제할 멤버의 프로필 이미지가 다릅니다.");
                s3Service.delete(imageUrl); // S3 삭제
                return ResponseEntity.ok("File deleted successfully");
            }
            log.info("기본 이미지와 삭제할 멤버의 프로필 이미지가 같습니다.");
            return ResponseEntity.ok("기본 이미지이므로 삭제를 하지 않습니당");
        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file");
        }
    }

//    @DeleteMapping(path = "/s3/test")
//    public ResponseEntity<String> deleteFile(@RequestParam(value = "email") String email) {
//        try {
//            String profileImgUrl = memberService.deleteMember(email); // 이미지와 멤버 엔티티 삭제
//            log.info("profileImgUrl는 by test? " + profileImgUrl);
//            s3Service.delete(profileImgUrl); // 이미지 S3에서 삭제
//            return ResponseEntity.ok("File deleted successfully");
//        } catch (Exception e) {
//            log.error("Failed to delete file: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file");
//        }
//    }
}