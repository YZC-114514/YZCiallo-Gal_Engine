import java.util.HashMap;
import java.util.Map;

/**
 * Game state manager for storing and managing game variables/flags.
 */
public class GameState {
    
    private static GameState instance;
    private final Map<String, Object> variables = new HashMap<>();
    
    private GameState() {}
    
    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    
    public void setFlag(String name, boolean value) {
        variables.put(name, value);
    }
    
    public boolean getFlag(String name) {
        Object value = variables.get(name);
        return value instanceof Boolean && (Boolean) value;
    }
    
    public void setInt(String name, int value) {
        variables.put(name, value);
    }
    
    public int getInt(String name) {
        Object value = variables.get(name);
        return value instanceof Integer ? (Integer) value : 0;
    }
    
    public void addInt(String name, int delta) {
        setInt(name, getInt(name) + delta);
    }
    
    public void setString(String name, String value) {
        variables.put(name, value);
    }
    
    public String getString(String name) {
        Object value = variables.get(name);
        return value instanceof String ? (String) value : "";
    }
    
    public boolean evaluateCondition(String condition) {
        if (condition == null || condition.isEmpty()) return true;
        condition = condition.trim();
        
        // Handle NOT
        if (condition.startsWith("!")) {
            return !evaluateCondition(condition.substring(1).trim());
        }
        
        // Handle comparisons
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                return String.valueOf(resolveValue(parts[0].trim()))
                    .equals(String.valueOf(resolveValue(parts[1].trim())));
            }
        }
        if (condition.contains("!=")) {
            String[] parts = condition.split("!=");
            if (parts.length == 2) {
                return !String.valueOf(resolveValue(parts[0].trim()))
                    .equals(String.valueOf(resolveValue(parts[1].trim())));
            }
        }
        if (condition.contains(">=")) {
            String[] parts = condition.split(">=");
            if (parts.length == 2) {
                return resolveInt(parts[0].trim()) >= resolveInt(parts[1].trim());
            }
        }
        if (condition.contains("<=")) {
            String[] parts = condition.split("<=");
            if (parts.length == 2) {
                return resolveInt(parts[0].trim()) <= resolveInt(parts[1].trim());
            }
        }
        if (condition.contains(">")) {
            String[] parts = condition.split(">");
            if (parts.length == 2) {
                return resolveInt(parts[0].trim()) > resolveInt(parts[1].trim());
            }
        }
        if (condition.contains("<")) {
            String[] parts = condition.split("<");
            if (parts.length == 2) {
                return resolveInt(parts[0].trim()) < resolveInt(parts[1].trim());
            }
        }
        
        // Simple flag check
        return getFlag(condition);
    }
    
    public void executeSet(String command) {
        if (command == null || command.isEmpty()) return;
        
        if (command.contains("+=")) {
            String[] parts = command.split("\\+=");
            if (parts.length == 2) {
                addInt(parts[0].trim(), resolveInt(parts[1].trim()));
                return;
            }
        }
        if (command.contains("-=")) {
            String[] parts = command.split("-=");
            if (parts.length == 2) {
                addInt(parts[0].trim(), -resolveInt(parts[1].trim()));
                return;
            }
        }
        if (command.contains("=")) {
            String[] parts = command.split("=", 2);
            if (parts.length == 2) {
                String varName = parts[0].trim();
                String valueStr = parts[1].trim();
                
                if (valueStr.equalsIgnoreCase("true")) {
                    setFlag(varName, true);
                } else if (valueStr.equalsIgnoreCase("false")) {
                    setFlag(varName, false);
                } else {
                    try {
                        setInt(varName, Integer.parseInt(valueStr));
                    } catch (NumberFormatException e) {
                        setString(varName, valueStr);
                    }
                }
                return;
            }
        }
        
        // Default: set flag to true
        setFlag(command.trim(), true);
    }
    
    private Object resolveValue(String expr) {
        expr = expr.trim();
        if (expr.equalsIgnoreCase("true")) return true;
        if (expr.equalsIgnoreCase("false")) return false;
        try { return Integer.parseInt(expr); } catch (NumberFormatException ignored) {}
        return variables.getOrDefault(expr, expr);
    }
    
    private int resolveInt(String expr) {
        Object value = resolveValue(expr);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        return 0;
    }
    
    public void clear() {
        variables.clear();
    }
    
    public Map<String, Object> getAllVariables() {
        return new HashMap<>(variables);
    }
    
    public void loadVariables(Map<String, Object> vars) {
        variables.clear();
        if (vars != null) variables.putAll(vars);
    }
}