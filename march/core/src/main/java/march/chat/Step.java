package march.chat;

public class Step {

    private int id;
    private String summary;

    public Step() {}

    public Step(int id, String summary) {
        this.id = id;
        this.summary = summary;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
