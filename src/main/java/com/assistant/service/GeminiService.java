package com.assistant.service;

import com.assistant.model.Flashcard;
import com.assistant.model.QuizQuestion;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service class for communicating with Google Gemini AI API.
 * Uses Java 11+ HttpClient and Gson for prompt generation and response parsing.
 */
public class GeminiService {

    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_INPUT_CHARS = 32000; // Cap to avoid huge HTTP payloads on slow connections

    private final HttpClient httpClient;
    private String apiKey;
    private String modelName;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        resolveConfiguration();
    }

    /**
     * Resolves the API key from GEMINI_API_KEY environment variable first,
     * falling back to config.properties (checked from disk then classloader) if not found.
     */
    public synchronized void resolveConfiguration() {
        this.apiKey = null;

        // 1. Priority: System Property or Environment Variable
        String sysProp = System.getProperty("gemini.api.key");
        if (isValidKey(sysProp)) {
            this.apiKey = sysProp.trim();
        }

        if (this.apiKey == null) {
            String envKey = System.getenv("GEMINI_API_KEY");
            if (isValidKey(envKey)) {
                this.apiKey = envKey.trim();
            }
        }

        // 2. Fallback: config.properties (checks disk first so updates apply immediately)
        Properties props = new Properties();
        java.io.File srcProps = new java.io.File("src/main/resources/config.properties");
        java.io.File targetProps = new java.io.File("target/classes/config.properties");

        if (srcProps.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(srcProps)) {
                props.load(fis);
            } catch (IOException ignored) {}
        } else if (targetProps.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(targetProps)) {
                props.load(fis);
            } catch (IOException ignored) {}
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException ignored) {}
        }

        if (this.apiKey == null) {
            String propKey = props.getProperty("gemini.api.key");
            if (isValidKey(propKey)) {
                this.apiKey = propKey.trim();
            }
        }

        String model = props.getProperty("gemini.api.model");
        if (model != null && !model.trim().isEmpty()) {
            this.modelName = model.trim();
        } else {
            this.modelName = DEFAULT_MODEL;
        }
    }

    private boolean isValidKey(String key) {
        return key != null && !key.trim().isEmpty() && !key.contains("YOUR_");
    }

    /**
     * Checks if a valid API key is currently detected.
     * Automatically attempts to re-resolve configuration if not yet loaded.
     */
    public synchronized boolean isApiKeyConfigured() {
        if (!isValidKey(this.apiKey)) {
            resolveConfiguration();
        }
        return isValidKey(this.apiKey);
    }

    /**
     * Generates a structured summary of research paper text in simple English.
     * 
     * @param paperText Extracted research paper text
     * @return AI-generated structured summary
     * @throws IOException If network or API fails
     */
    public String generateSummary(String paperText) throws IOException, InterruptedException {
        validateInput(paperText, "Research paper text is empty. Please upload a valid paper.");

        // Handle very long research papers by focusing on core content
        String truncatedText = truncateIfNeeded(paperText);

        String prompt = "You are an expert AI Research Paper Assistant helping college computer engineering students.\n"
                + "Please read the following research paper text and generate a simple, clear, and comprehensive summary.\n"
                + "Structure your response with these exact markdown sections:\n\n"
                + "### 1. Research Objective\n(Explain the problem being solved in 2-3 sentences)\n\n"
                + "### 2. Methodology & Approach\n(Explain the techniques, algorithms, or architecture proposed)\n\n"
                + "### 3. Key Findings & Contributions\n(Highlight the core results, performance improvements, or innovations)\n\n"
                + "### 4. Conclusion & Real-World Impact\n(Summarize why this paper matters and how it applies to practical computing)\n\n"
                + "Use simple English suitable for a college viva presentation. Do NOT make up facts not present in the text.\n\n"
                + "Research Paper Content:\n" + truncatedText;

        return callGeminiApi(prompt);
    }

    /**
     * Explains a specific difficult paragraph in simple English with technical term definitions.
     * 
     * @param paragraphText Selected complex paragraph
     * @return Simple explanation for students
     * @throws IOException If network or API fails
     */
    public String explainParagraph(String paragraphText) throws IOException, InterruptedException {
        validateInput(paragraphText, "Please select or paste a paragraph to explain.");

        String prompt = "You are a patient and knowledgeable college engineering professor.\n"
                + "Explain the following difficult paragraph from a research paper in simple English for a 3rd-year student.\n"
                + "- Break down any confusing mathematical notations or advanced technical terms.\n"
                + "- Use an everyday analogy if appropriate.\n"
                + "- Preserve the exact original scientific meaning without distorting facts.\n\n"
                + "Paragraph to explain:\n" + paragraphText.trim();

        return callGeminiApi(prompt);
    }

    /**
     * Answers a student's question strictly using the research paper text as context.
     * 
     * @param paperText    The extracted text of the paper
     * @param userQuestion The student's question
     * @return Grounded AI answer, or explicit refusal if answer is not in paper
     * @throws IOException If network or API fails
     */
    public String askQuestion(String paperText, String userQuestion) throws IOException, InterruptedException {
        validateInput(paperText, "Research paper text is empty. Cannot answer questions.");
        validateInput(userQuestion, "Please enter a question to ask.");

        String truncatedText = truncateIfNeeded(paperText);

        String prompt = "You are an AI Research Paper Assistant helping a student understand this specific research paper.\n\n"
                + "Instructions:\n"
                + "1. Answer the student's question based strictly on the research paper content provided below.\n"
                + "2. If the answer is not contained in or cannot be directly inferred from the paper text, respond with:\n"
                + "   \"The provided research paper does not contain information to answer this question.\"\n"
                + "3. Keep your explanation clear, precise, and educational for a college engineering student.\n"
                + "4. Do NOT hallucinate or assume facts beyond the paper text.\n\n"
                + "Research Paper Content:\n" + truncatedText + "\n\n"
                + "Student Question: " + userQuestion.trim();

        return callGeminiApi(prompt);
    }

    /**
     * Core method to dispatch HTTP POST request to Google Gemini API.
     * Includes automatic fallback and retry if a model encounters temporary demand spikes (HTTP 503).
     */
    public String callGeminiApi(String prompt) throws IOException, InterruptedException {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException(
                    "Google Gemini API key is missing!\n\n"
                    + "Please configure your key using either:\n"
                    + "1. Environment variable: Set GEMINI_API_KEY (e.g. setx GEMINI_API_KEY \"your_key\")\n"
                    + "2. Or edit src/main/resources/config.properties and set gemini.api.key=your_key\n\n"
                    + "You can get a free API key at: https://aistudio.google.com/"
            );
        }

        // Primary model with automatic fallback
        String primary = (modelName != null && !modelName.trim().isEmpty()) ? modelName.trim() : DEFAULT_MODEL;
        String fallback = primary.equals("gemini-3.5-flash-lite") ? "gemini-3.5-flash" : "gemini-3.5-flash-lite";
        String[] candidateModels = new String[]{primary, fallback};

        IOException lastException = null;

        for (String targetModel : candidateModels) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    return executeRequest(targetModel, prompt);
                } catch (IOException e) {
                    lastException = e;
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    boolean isTransient = msg.contains("503") || msg.contains("500") || msg.contains("429");
                    if (isTransient && attempt < 2) {
                        Thread.sleep(1200);
                    } else if (!isTransient) {
                        throw e;
                    }
                }
            }
        }

        if (lastException != null) {
            String msg = lastException.getMessage();
            if (msg != null && msg.contains("503")) {
                throw new IOException("Google AI service is currently experiencing high temporary demand (HTTP 503). Please click 'Generate AI Summary' again in a few moments.");
            }
            throw lastException;
        }

        throw new IOException("Failed to obtain response from Gemini API.");
    }

    private String executeRequest(String targetModel, String prompt) throws IOException, InterruptedException {
        String endpoint = API_BASE_URL + targetModel + ":generateContent?key=" + apiKey;

        // Build Gemini Request Payload using Gson
        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        root.add("contents", contents);

        // Generation Config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.3);
        genConfig.addProperty("maxOutputTokens", 2048);
        root.add("generationConfig", genConfig);

        String jsonPayload = root.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        String responseBody = response.body();

        // Error status checks
        if (statusCode == 429) {
            throw new IOException("Gemini API rate limit exceeded (HTTP 429). Please wait a few seconds before retrying.");
        } else if (statusCode == 403 || statusCode == 400) {
            String errorDetail = extractErrorMessage(responseBody);
            throw new IOException("Gemini API Request Error (HTTP " + statusCode + "): " + errorDetail);
        } else if (statusCode >= 500) {
            throw new IOException("Google AI server error (HTTP " + statusCode + "). Please try again shortly.");
        } else if (statusCode != 200) {
            throw new IOException("Unexpected response from Gemini API (HTTP " + statusCode + "): " + responseBody);
        }

        return parseGeminiResponse(responseBody);
    }

    private String parseGeminiResponse(String responseJson) throws IOException {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();

            if (json.has("candidates")) {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content")) {
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
                            }
                        }
                    }
                }
            }
            throw new IOException("Received empty response from Gemini AI. Please try again.");
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error")) {
                JsonObject err = json.getAsJsonObject("error");
                if (err.has("message")) {
                    return err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    private void validateInput(String input, String errorMessage) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String truncateIfNeeded(String text) {
        if (text.length() <= MAX_INPUT_CHARS) {
            return text;
        }
        // Retain first 25k chars (Abstract + Intro + Architecture) and last 6k chars (Results + Conclusion)
        String head = text.substring(0, 24000);
        String tail = text.substring(text.length() - 6000);
        return head + "\n\n[... Note: Middle sections condensed for length ...]\n\n" + tail;
    }

    /**
     * Generates a list of multiple-choice quiz questions from the research paper.
     *
     * @param paperText    Extracted text of the research paper
     * @param numQuestions Desired number of questions (5 or 10)
     * @return List of QuizQuestion objects
     * @throws IOException If API fails or response parsing fails
     */
    public List<QuizQuestion> generateQuiz(String paperText, int numQuestions) throws IOException, InterruptedException {
        validateInput(paperText, "Research paper text is empty. Cannot generate quiz.");
        int count = (numQuestions == 10) ? 10 : 5;
        String truncatedText = truncateIfNeeded(paperText);

        String prompt = "You are an expert academic evaluator and AI Research Paper Assistant.\n"
                + "Create exactly " + count + " high-quality multiple-choice questions (MCQs) grounded strictly in the research paper below.\n\n"
                + "Requirements:\n"
                + "1. Each question must test comprehension of core concepts, methodology, algorithms, findings, or performance metrics described in the paper.\n"
                + "2. Provide exactly four options: optionA, optionB, optionC, optionD.\n"
                + "3. Provide exactly ONE correct answer (must be \"A\", \"B\", \"C\", or \"D\").\n"
                + "4. Provide a clear 1-2 sentence explanation citing the paper's findings.\n"
                + "5. Do NOT invent facts or hallucinate details absent from the paper.\n"
                + "6. You MUST respond with ONLY a valid JSON array of objects. Do NOT include markdown code fences, headers, or any other text.\n\n"
                + "JSON Schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Question text?\",\n"
                + "    \"optionA\": \"Choice A\",\n"
                + "    \"optionB\": \"Choice B\",\n"
                + "    \"optionC\": \"Choice C\",\n"
                + "    \"optionD\": \"Choice D\",\n"
                + "    \"correctAnswer\": \"A\",\n"
                + "    \"explanation\": \"Brief explanation grounded in paper.\"\n"
                + "  }\n"
                + "]\n\n"
                + "Research Paper Content:\n" + truncatedText;

        String rawResponse = callGeminiApi(prompt);
        return parseQuizResponse(rawResponse);
    }

    private List<QuizQuestion> parseQuizResponse(String response) throws IOException {
        String cleaned = cleanJsonString(response);
        List<QuizQuestion> questions = new ArrayList<>();

        try {
            JsonArray array = JsonParser.parseString(cleaned).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                String qText = getJsonString(obj, "question");
                String optA = getJsonString(obj, "optionA");
                String optB = getJsonString(obj, "optionB");
                String optC = getJsonString(obj, "optionC");
                String optD = getJsonString(obj, "optionD");
                String correct = getJsonString(obj, "correctAnswer");
                String explanation = getJsonString(obj, "explanation");

                if (qText.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                    continue;
                }

                correct = correct.replaceAll("[^ABCDabcd]", "").toUpperCase();
                if (correct.isEmpty()) {
                    correct = "A";
                }

                questions.add(new QuizQuestion(qText, optA, optB, optC, optD, correct, explanation));
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse quiz questions from AI response: " + e.getMessage(), e);
        }

        if (questions.isEmpty()) {
            throw new IOException("AI generated an invalid or empty quiz format. Please try again.");
        }

        return questions;
    }

    /**
     * Generates 10 question-and-answer study flashcards from the research paper.
     *
     * @param paperText Extracted text of the research paper
     * @return List of Flashcard objects
     * @throws IOException If API fails or response parsing fails
     */
    public List<Flashcard> generateFlashcards(String paperText) throws IOException, InterruptedException {
        validateInput(paperText, "Research paper text is empty. Cannot generate flashcards.");
        String truncatedText = truncateIfNeeded(paperText);

        String prompt = "You are an expert AI Research Paper Assistant.\n"
                + "Generate exactly 10 question-and-answer study flashcards grounded strictly in the research paper below.\n\n"
                + "Requirements:\n"
                + "1. Each flashcard should test key definitions, architecture components, problems solved, or performance metrics.\n"
                + "2. The 'question' should be concise (front of card).\n"
                + "3. The 'answer' should be a clear, informative explanation (back of card) strictly from the paper.\n"
                + "4. Do NOT hallucinate facts absent from the text.\n"
                + "5. You MUST respond with ONLY a valid JSON array of objects. Do NOT include markdown code fences, headers, or any other text.\n\n"
                + "JSON Schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Key question or term?\",\n"
                + "    \"answer\": \"Clear, concise answer from paper.\"\n"
                + "  }\n"
                + "]\n\n"
                + "Research Paper Content:\n" + truncatedText;

        String rawResponse = callGeminiApi(prompt);
        return parseFlashcardsResponse(rawResponse);
    }

    private List<Flashcard> parseFlashcardsResponse(String response) throws IOException {
        String cleaned = cleanJsonString(response);
        List<Flashcard> cards = new ArrayList<>();

        try {
            JsonArray array = JsonParser.parseString(cleaned).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                String qText = getJsonString(obj, "question");
                String aText = getJsonString(obj, "answer");

                if (!qText.isEmpty() && !aText.isEmpty()) {
                    cards.add(new Flashcard(qText, aText));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse flashcards from AI response: " + e.getMessage(), e);
        }

        if (cards.isEmpty()) {
            throw new IOException("AI generated an invalid or empty flashcard format. Please try again.");
        }

        return cards;
    }

    private String cleanJsonString(String response) {
        if (response == null) return "[]";
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        }
        cleaned = cleaned.trim();

        int startBracket = cleaned.indexOf('[');
        int endBracket = cleaned.lastIndexOf(']');
        if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
            cleaned = cleaned.substring(startBracket, endBracket + 1);
        }

        return cleaned;
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString().trim();
        }
        return "";
    }
}
