package shared;



// https://hyperskill.org/learn/step/3712

public interface ValidityChecker {
    public default boolean checkValidityOfName(String name) {
        return checkValidityOfNickname(name);
    }

    public default boolean checkValidityOfNickname(String nickname) {
        if (nickname.length() < 3) {
            return false;
        }
        return true;
    }

    public default boolean checkValidityOfPassword(String password) {
        if (password.length() < 3 || password.matches("(.)\\1*")) {
            return false;
        }
        return true;
    }

    public default boolean checkValidityOfText(String text) {
        if (text.isEmpty() || text.isBlank()) {
            return false;
        }
        return true;
    }
}
