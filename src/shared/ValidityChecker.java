package shared;



// https://hyperskill.org/learn/step/3712

public interface ValidityChecker {
    String disallowedSpaceCharacter = "\u202F";
    String privateChatIndicator = "Private Verbindung";
    public default boolean checkValidityOfName(String name) {
        return checkValidityOfNickname(name);
    }

    public default boolean checkValidityOfNickname(String nickname) {
        if (nickname.length() < 3 || nickname.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }

    public default boolean checkValidityOfPassword(String password) {
        if (password.length() < 3 || password.matches("(.)\\1*") || password.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }

    public default boolean checkValidityOfText(String text) {
        if (text.isEmpty() || text.isBlank() || text.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }
    public default boolean checkValidityOfPublicGroup(String group) {
        if (group.isEmpty() || group.isBlank() || group.contains(privateChatIndicator)) {
            return false;
        }
        return true;
    }

}
