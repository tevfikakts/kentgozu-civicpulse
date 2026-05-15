package com.tevfik.kentgozu.ai_analysis.controller;

import com.tevfik.kentgozu.ai_analysis.service.VisionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketAnalysisController {

    private final VisionAnalysisService visionAnalysisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeTicketImage(@RequestParam("file") MultipartFile file) throws IOException {
        var result = visionAnalysisService.analyze(file);
        return ResponseEntity.ok(result);
    }
}
