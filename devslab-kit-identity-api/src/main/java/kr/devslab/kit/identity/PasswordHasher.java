package kr.devslab.kit.identity;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashed);
}
