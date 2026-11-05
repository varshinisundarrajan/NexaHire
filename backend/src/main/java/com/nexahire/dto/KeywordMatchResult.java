package com.nexahire.dto;

import java.util.List;

public class KeywordMatchResult {
    private int score;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private int totalRequired;
    private int totalMatched;

    public KeywordMatchResult() {}

    public KeywordMatchResult(int score, List<String> matchedSkills, List<String> missingSkills, int totalRequired, int totalMatched) {
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.totalRequired = totalRequired;
        this.totalMatched = totalMatched;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public int getTotalRequired() { return totalRequired; }
    public void setTotalRequired(int totalRequired) { this.totalRequired = totalRequired; }

    public int getTotalMatched() { return totalMatched; }
    public void setTotalMatched(int totalMatched) { this.totalMatched = totalMatched; }
}
