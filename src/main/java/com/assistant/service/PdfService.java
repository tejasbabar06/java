package com.assistant.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/**
 * Service class for processing PDF files and extracting text content using Apache PDFBox 3.
 * Handles validation, password-protected PDFs, corrupt files, and scanned empty documents.
 */
public class PdfService {

    /**
     * Extracts all text from a selected PDF file.
     * 
     * @param pdfFile The PDF file chosen by the student
     * @return Extracted plain-text content
     * @throws IOException If file is missing, corrupt, encrypted, or contains no readable text
     */
    public String extractText(File pdfFile) throws IOException {
        // 1. File existence validation
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.isFile()) {
            throw new IOException("The selected file does not exist or is invalid.");
        }

        // 2. Extension validation
        String fileName = pdfFile.getName().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            throw new IOException("Invalid file format. Please select a valid .pdf file.");
        }

        // 3. File size check (avoid zero-byte empty files)
        if (pdfFile.length() == 0) {
            throw new IOException("The selected PDF file is completely empty (0 bytes).");
        }

        // 4. Load PDF using Apache PDFBox 3 Loader
        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            // Check for password protection / encryption
            if (document.isEncrypted()) {
                throw new IOException("The PDF is password-protected or encrypted. Please remove the password and try again.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new IOException("The PDF contains no pages.");
            }

            // 5. Extract text across all pages
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true); // Ensures natural reading order
            String extractedText = textStripper.getText(document);

            // 6. Check if extracted text is blank (common for scanned documents)
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IOException("No extractable text was found in this PDF. It appears to be a scanned document or image-only file. Please upload a text-based PDF.");
            }

            return extractedText.trim();
        } catch (IOException e) {
            // Re-throw with clear message
            if (e.getMessage() != null && (e.getMessage().contains("password") || e.getMessage().contains("scanned"))) {
                throw e;
            }
            throw new IOException("Could not read PDF: " + (e.getMessage() != null ? e.getMessage() : "File may be damaged or corrupt."), e);
        }
    }

    /**
     * Helper to compute word count of extracted text.
     */
    public int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
}
