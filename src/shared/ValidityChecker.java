package shared;

public interface ValidityChecker {
    String disallowedSpaceCharacter = "\u202F";
    String privateChatIndicator = "Private Verbindung von ";
    default boolean checkValidityOfName(String name) {
        return checkValidityOfNickname(name);
    }

    default boolean checkValidityOfNickname(String nickname) {
        if (nickname.length() < 3 || nickname.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }

    default boolean checkValidityOfPassword(String password) {
        if (password.length() < 3 || password.matches("(.)\\1*") || password.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }

    default boolean checkValidityOfText(String text) {
        if (text.isEmpty() || text.isBlank() || text.contains(disallowedSpaceCharacter)) {
            return false;
        }
        return true;
    }
    default boolean checkValidityOfPublicGroup(String group) {
        if (group.isEmpty() || group.isBlank() || group.contains(privateChatIndicator)) {
            return false;
        }
        return true;
    }
}