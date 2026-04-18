package googol;

import java.io.Serializable;

public class wordCount implements Serializable {
    private static final long serialVersionUID = 1L; // Add a unique ID for serialization

    private String word;
    private int count;

    public wordCount(String word, int count) {
        super();
        this.word = word;
        this.count = count;
    }

    public String getWord() {
        return word;
    }

    public int getCount() {
        return count;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setCount(int count) {
        this.count = count;
    }

}