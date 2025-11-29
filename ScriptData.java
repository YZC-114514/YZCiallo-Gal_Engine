/**
 * Data class representing a single script command/action.
 * Uses proper encapsulation for better maintainability.
 * 
 * Supported command types:
 * - DIALOGUE: Display dialogue (name, mood, text)
 * - BG: Change background image (param = image path)
 * - CHAR: Show/change character sprite (name, mood, param = position: left/center/right)
 * - CHAR_HIDE: Hide character (name or param = position)
 * - CHOICE: Display choice options (text = JSON array of choices)
 * - GOTO: Jump to label (param = label name)
 * - LABEL: Define jump target (param = label name)
 * - NEXT_CHAPTER: Go to next chapter file (param = filename)
 * - BGM: Play background music (param = audio file, text = "stop" to stop)
 * - SE: Play sound effect (param = audio file)
 * - SET: Set game variable (param = assignment expression)
 * - IF: Conditional execution (param = condition, text = goto label if true)
 * - EFFECT: Screen effect (param = effect type: fade/flash/shake, text = parameters)
 * - WAIT: Pause execution (param = milliseconds)
 */
public class ScriptData {
    
    // Command type constants
    public static final String TYPE_DIALOGUE = "DIALOGUE";
    public static final String TYPE_BG = "BG";
    public static final String TYPE_CHAR = "CHAR";
    public static final String TYPE_CHAR_HIDE = "CHAR_HIDE";
    public static final String TYPE_CHOICE = "CHOICE";
    public static final String TYPE_GOTO = "GOTO";
    public static final String TYPE_LABEL = "LABEL";
    public static final String TYPE_NEXT_CHAPTER = "NEXT_CHAPTER";
    public static final String TYPE_BGM = "BGM";
    public static final String TYPE_SE = "SE";
    public static final String TYPE_SET = "SET";
    public static final String TYPE_IF = "IF";
    public static final String TYPE_EFFECT = "EFFECT";
    public static final String TYPE_WAIT = "WAIT";
    public static final String TYPE_VAR ="VAR";
    
    // Character position constants
    public static final String POS_LEFT = "left";
    public static final String POS_CENTER = "center";
    public static final String POS_RIGHT = "right";
    
    // Fields with package-private access for Gson serialization
    public String type;   // Command type
    public String name;   // Character name (for dialogue/char)
    public String mood;   // Character emotion/expression
    public String text;   // Dialogue text or secondary parameter
    public String param;  // Primary parameter (image, label, condition, etc.)

    // Default constructor for Gson
    public ScriptData() {}

    // Convenience constructor
    public ScriptData(String type, String name, String mood, String text, String param) {
        this.type = type;
        this.name = name;
        this.mood = mood;
        this.text = text;
        this.param = param;
    }

    // Getters for safer access
    public String getType() { return type; }
    public String getName() { return name; }
    public String getMood() { return mood; }
    public String getText() { return text; }
    public String getParam() { return param; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setMood(String mood) { this.mood = mood; }
    public void setText(String text) { this.text = text; }
    public void setParam(String param) { this.param = param; }

    /**
     * Checks if this is a specific command type (case-insensitive).
     */
    public boolean isType(String typeToCheck) {
        return type != null && type.equalsIgnoreCase(typeToCheck);
    }


    /**
     * factory methods for Favorability System
     */
    public static ScriptData varAdd(String key, int value) {
        // 自動組裝字串，防止手誤
        String commandString = "ADD, " + key + ", " + value;
        return new ScriptData(TYPE_VAR, null, null, commandString, null);
    }

    public static ScriptData varSet(String key, int value) {
        String commandString = "SET, " + key + ", " + value;
        return new ScriptData(TYPE_VAR, null, null, commandString, null);
    }

    public static ScriptData varCheck(String key, int threshold, String targetLabel) {
        String commandString = "View, " + key + ", " + threshold + ", " + targetLabel;
        return new ScriptData(TYPE_VAR, null, null, commandString, null);
    }


    /**
     * Factory methods for common command types
     */
    public static ScriptData dialogue(String name, String mood, String text) {
        return new ScriptData(TYPE_DIALOGUE, name, mood, text, null);
    }

    public static ScriptData background(String imagePath) {
        return new ScriptData(TYPE_BG, null, null, null, imagePath);
    }

    public static ScriptData character(String name, String mood, String position) {
        return new ScriptData(TYPE_CHAR, name, mood, null, position);
    }

    public static ScriptData hideCharacter(String nameOrPosition) {
        return new ScriptData(TYPE_CHAR_HIDE, null, null, null, nameOrPosition);
    }

    public static ScriptData bgm(String audioFile) {
        return new ScriptData(TYPE_BGM, null, null, null, audioFile);
    }

    public static ScriptData stopBgm() {
        return new ScriptData(TYPE_BGM, null, null, "stop", null);
    }

    public static ScriptData soundEffect(String audioFile) {
        return new ScriptData(TYPE_SE, null, null, null, audioFile);
    }

    public static ScriptData setVariable(String expression) {
        return new ScriptData(TYPE_SET, null, null, null, expression);
    }

    public static ScriptData ifCondition(String condition, String gotoLabel) {
        return new ScriptData(TYPE_IF, null, null, gotoLabel, condition);
    }

    public static ScriptData effect(String effectType, String params) {
        return new ScriptData(TYPE_EFFECT, null, null, params, effectType);
    }

    public static ScriptData wait(int milliseconds) {
        return new ScriptData(TYPE_WAIT, null, null, null, String.valueOf(milliseconds));
    }

    public static ScriptData label(String labelName) {
        return new ScriptData(TYPE_LABEL, null, null, null, labelName);
    }

    public static ScriptData gotoLabel(String labelName) {
        return new ScriptData(TYPE_GOTO, null, null, null, labelName);
    }

    @Override
    public String toString() {
        return "ScriptData{" +
            "type='" + type + '\'' +
            ", name='" + name + '\'' +
            ", mood='" + mood + '\'' +
            ", text='" + (text != null && text.length() > 20 ? text.substring(0, 20) + "..." : text) + '\'' +
            ", param='" + param + '\'' +
            '}';
    }
}
