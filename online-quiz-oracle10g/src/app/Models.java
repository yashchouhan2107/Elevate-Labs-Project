package app;

public class Models {
    public static class Question {
        public long questionId;
        public String text;
        public java.util.List<Option> options = new java.util.ArrayList<Option>();
    }
    public static class Option {
        public long optionId;
        public String text;
        public boolean isCorrect;
    }
}
