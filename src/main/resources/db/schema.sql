-- ==============================================================================
-- AI Research Paper Assistant - Database Schema
-- Database: ai_research_assistant
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS ai_research_assistant;
USE ai_research_assistant;

-- 1. Users Table (User registration & login details)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Papers Table (Metadata, file location, and extracted text from PDF)
CREATE TABLE IF NOT EXISTS papers (
    paper_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) DEFAULT 'Unknown',
    file_path VARCHAR(500) NOT NULL,
    extracted_text LONGTEXT,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_papers_user FOREIGN KEY (user_id) 
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Summaries Table (AI-generated summaries of uploaded papers)
CREATE TABLE IF NOT EXISTS summaries (
    summary_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_id INT NOT NULL,
    summary_text LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_summaries_paper FOREIGN KEY (paper_id) 
        REFERENCES papers(paper_id) ON DELETE CASCADE
);

-- 4. Notes Table (Student notes associated with research papers)
CREATE TABLE IF NOT EXISTS notes (
    note_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_id INT NOT NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_paper FOREIGN KEY (paper_id) 
        REFERENCES papers(paper_id) ON DELETE CASCADE
);

-- 5. Quiz Table (Multiple-choice questions generated from paper content)
CREATE TABLE IF NOT EXISTS quiz (
    quiz_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_id INT NOT NULL,
    question TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_answer VARCHAR(10) NOT NULL,
    CONSTRAINT fk_quiz_paper FOREIGN KEY (paper_id) 
        REFERENCES papers(paper_id) ON DELETE CASCADE
);

-- 6. Chat History Table (Student Q&A interactions with AI assistant)
CREATE TABLE IF NOT EXISTS chat_history (
    chat_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_id INT NOT NULL,
    user_question TEXT NOT NULL,
    ai_answer LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_paper FOREIGN KEY (paper_id) 
        REFERENCES papers(paper_id) ON DELETE CASCADE
);
