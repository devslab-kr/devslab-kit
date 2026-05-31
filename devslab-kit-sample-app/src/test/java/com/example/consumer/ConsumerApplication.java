package com.example.consumer;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Stand-in for a real external consumer of devslab-kit: a <strong>plain</strong>
 * {@code @SpringBootApplication} in a package that is <em>not</em> under
 * {@code kr.devslab.kit}. No {@code scanBasePackages}, no {@code @EntityScan}, no
 * {@code @EnableJpaRepositories}.
 *
 * <p>If the kit's {@code PersistenceAutoConfiguration} did not auto-register the
 * kit's entities + repositories, booting from here would fail with
 * {@code Not a managed type: …PlatformUserAccountEntity}. The test alongside this
 * class proves that (a) the kit works from an unrelated package AND (b) this
 * consumer's own {@code @Entity}/repository in {@code com.example.consumer} still
 * work — i.e. the kit broadened scanning rather than replacing it.
 */
@SpringBootApplication
public class ConsumerApplication {
}
