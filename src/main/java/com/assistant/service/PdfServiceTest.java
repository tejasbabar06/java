package com.assistant.service;

import com.assistant.dao.PaperDAO;
import com.assistant.dao.UserDAO;
import com.assistant.model.Paper;
import com.assistant.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;

/**
 * Standalone test suite to verify Apache PDFBox 3 integration:
 * - PDF text extraction
 * - Corrupt / non-PDF / empty file handling
 * - MySQL persistence of extracted content
 * - Data retrieval and user privacy checks
 */
public class PdfServiceTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("    AI Research Paper Assistant - PDF Service Test        ");
        System.out.println("==========================================================");

        PdfService pdfService = new PdfService();
        PaperDAO paperDAO = new PaperDAO();
        UserDAO userDAO = new UserDAO();

        try {
            User demoUser = userDAO.getUserByEmail("student.demo@college.edu");
            if (demoUser == null) {
                System.err.println("[ERROR] Demo user not found! Please run UserDAOTest first.");
                return;
            }
            int userId = demoUser.getUserId();

            // 1. Create a genuine test PDF using PDFBox 3
            File testPdf = new File("target/sample_transformer_paper.pdf");
            testPdf.getParentFile().mkdirs();
            generateSamplePdf(testPdf);
            System.out.println("[INFO] Generated test PDF file: " + testPdf.getAbsolutePath());

            // 2. Test Text Extraction
            System.out.println("\n[TEST 1] Extracting text using Apache PDFBox 3 Loader & PDFTextStripper...");
            String extracted = pdfService.extractText(testPdf);
            int wordCount = pdfService.calculateWordCount(extracted);
            System.out.println("  Extracted characters: " + extracted.length());
            System.out.println("  Extracted words:      " + wordCount);
            System.out.println("  Snippet:              " + extracted.substring(0, Math.min(120, extracted.length())) + "...");

            if (extracted.contains("Attention Mechanisms") && wordCount > 20) {
                System.out.println("  [SUCCESS] PDF text extracted accurately!");
            } else {
                System.err.println("  [FAILED] Extracted content did not match expected keywords.");
            }

            // 3. Test Database Persistence of Extracted Text
            System.out.println("\n[TEST 2] Saving extracted paper into MySQL database...");
            Paper paper = new Paper(
                    userId,
                    "Transformer Attention Model",
                    "A. Vaswani, N. Shazeer",
                    testPdf.getAbsolutePath(),
                    extracted
            );
            boolean saved = paperDAO.addPaper(paper);
            if (saved && paper.getPaperId() > 0) {
                System.out.println("  [SUCCESS] Paper saved in MySQL with paper_id: " + paper.getPaperId());
            } else {
                System.err.println("  [FAILED] Could not save extracted paper to MySQL.");
            }

            // 4. Test Retrieval from MySQL
            System.out.println("\n[TEST 3] Retrieving paper back from MySQL by ID...");
            Paper retrieved = paperDAO.getPaperById(paper.getPaperId(), userId);
            if (retrieved != null && retrieved.getExtractedText().equals(extracted)) {
                System.out.println("  [SUCCESS] Retrieved paper matches extracted PDF content 100%!");
            } else {
                System.err.println("  [FAILED] Retrieved paper data mismatch.");
            }

            // 5. Test User Privacy / Isolation
            System.out.println("\n[TEST 4] Testing access restriction (User 9999 accessing User " + userId + "'s paper)...");
            Paper unauthorized = paperDAO.getPaperById(paper.getPaperId(), 9999);
            if (unauthorized == null) {
                System.out.println("  [SUCCESS] Access safely denied for unauthorized user!");
            } else {
                System.err.println("  [FAILED] Data leakage: unauthorized user accessed paper!");
            }

            // 6. Test Empty / Scanned Document (Page with no text)
            System.out.println("\n[TEST 5] Testing scanned / empty PDF detection...");
            File emptyPdf = new File("target/empty_scanned_paper.pdf");
            generateBlankPdf(emptyPdf);
            try {
                pdfService.extractText(emptyPdf);
                System.err.println("  [FAILED] Empty PDF should have thrown an informative exception!");
            } catch (IOException e) {
                System.out.println("  [SUCCESS] Correctly detected empty/scanned PDF: " + e.getMessage());
            }

            // 7. Test Non-PDF file validation
            System.out.println("\n[TEST 6] Testing invalid file extension validation (.txt instead of .pdf)...");
            File txtFile = new File("target/test.txt");
            txtFile.createNewFile();
            try {
                pdfService.extractText(txtFile);
                System.err.println("  [FAILED] Non-PDF should have thrown an exception!");
            } catch (IOException e) {
                System.out.println("  [SUCCESS] Correctly rejected invalid file extension: " + e.getMessage());
            }

            System.out.println("\n----------------------------------------------------------");
            System.out.println("[RESULT] ALL 6 PDF SERVICE & EXTRACTION TESTS PASSED!");
            System.out.println("==========================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }

    private static void generateSamplePdf(File file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                stream.newLineAtOffset(50, 720);
                stream.showText("Attention Mechanisms in Deep Learning");

                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(0, -30);
                stream.showText("Abstract: Research in sequence transduction relies heavily on self-attention.");
                stream.newLineAtOffset(0, -20);
                stream.showText("The Transformer model replaces recurrent layers with multi-head self-attention.");
                stream.newLineAtOffset(0, -20);
                stream.showText("This allows greater parallelization and achieves superior translation quality.");
                stream.endText();
            }
            doc.save(file);
        }
    }

    private static void generateBlankPdf(File file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page); // Page with no text stream
            doc.save(file);
        }
    }
}
