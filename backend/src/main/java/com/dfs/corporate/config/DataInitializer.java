package com.dfs.corporate.config;

import com.dfs.corporate.domain.*;
import com.dfs.corporate.repository.AccountPortalRoleRepository;
import com.dfs.corporate.repository.AccountRepository;
import com.dfs.corporate.repository.BrandRepository;
import com.dfs.corporate.repository.PartyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEMO_PASSWORD = "Admin@123";

    @Bean
    CommandLineRunner seedAdmin(PartyRepository partyRepository,
                                AccountRepository accountRepository,
                                AccountPortalRoleRepository portalRoleRepository,
                                BrandRepository brandRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            brandRepository.findByCodeIgnoreCase("ZINDIGI").ifPresent(dfs ->
                    partyRepository.findAll().stream()
                            .filter(p -> p.getBrandId() == null)
                            .forEach(p -> {
                                p.setBrandId(dfs.getId());
                                partyRepository.save(p);
                            }));

            Party platformParty = ensurePlatformParty(partyRepository, brandRepository, accountRepository);
            ensureAccount(accountRepository, portalRoleRepository, passwordEncoder,
                    platformParty.getId(), "admin@zindigicorp.local", Role.PLATFORM_ADMIN, null);

            Party merchantParty = partyRepository.findAll().stream()
                    .filter(p -> p.getPartyType() == PartyType.MERCHANT && p.getStatus() == PartyStatus.ACTIVE)
                    .filter(p -> p.getBusinessName() != null
                            && p.getBusinessName().toLowerCase().contains("zindigi"))
                    .findFirst()
                    .orElse(platformParty);

            ensureAccount(accountRepository, portalRoleRepository, passwordEncoder,
                    merchantParty.getId(), "ops@zindigicorp.local", Role.PARTY_USER, PortalRole.FINANCING_OPS);
            ensureAccount(accountRepository, portalRoleRepository, passwordEncoder,
                    merchantParty.getId(), "checker@zindigicorp.local", Role.PARTY_USER, PortalRole.FINANCING_CHECKER);
            ensureAccount(accountRepository, portalRoleRepository, passwordEncoder,
                    merchantParty.getId(), "merchant@zindigicorp.local", Role.PARTY_USER, null);

            accountRepository.findByEmailIgnoreCase("admin@zindigicorp.local").ifPresent(admin ->
                    ensureRole(portalRoleRepository, admin.getId(), PortalRole.FINANCING_OPS));
            accountRepository.findByEmailIgnoreCase("admin@dfscorporate.local").ifPresent(admin ->
                    ensureRole(portalRoleRepository, admin.getId(), PortalRole.FINANCING_CHECKER));
        };
    }

    private Party ensurePlatformParty(PartyRepository partyRepository,
                                      BrandRepository brandRepository,
                                      AccountRepository accountRepository) {
        return accountRepository.findByEmailIgnoreCase("admin@zindigicorp.local")
                .flatMap(account -> partyRepository.findById(account.getPartyId()))
                .orElseGet(() -> {
                    Party party = new Party();
                    party.setPublicId(UUID.randomUUID().toString());
                    party.setPartyType(PartyType.MERCHANT);
                    party.setStatus(PartyStatus.ACTIVE);
                    party.setFullName("Platform Admin");
                    party.setBusinessName("Zindigi Corporate");
                    party.setEmail("admin@zindigicorp.local");
                    party.setPhone("0000000000");
                    party.setCountry("Pakistan");
                    brandRepository.findByCodeIgnoreCase("ZINDIGI")
                            .map(Brand::getId)
                            .ifPresent(party::setBrandId);
                    return partyRepository.save(party);
                });
    }

    private void ensureAccount(AccountRepository accountRepository,
                               AccountPortalRoleRepository portalRoleRepository,
                               PasswordEncoder passwordEncoder,
                               Long partyId,
                               String email,
                               Role role,
                               PortalRole portalRole) {
        Account account = accountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Account created = new Account();
            created.setPublicId(UUID.randomUUID().toString());
            created.setPartyId(partyId);
            created.setEmail(email);
            created.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            created.setRole(role);
            created.setStatus(AccountStatus.ACTIVE);
            created.setFirstLogin(false);
            Account saved = accountRepository.save(created);
            log.info("Seeded demo account: {} / {}", email, DEMO_PASSWORD);
            return saved;
        });
        if (portalRole != null) {
            ensureRole(portalRoleRepository, account.getId(), portalRole);
        }
    }

    private void ensureRole(AccountPortalRoleRepository portalRoleRepository, Long accountId, PortalRole role) {
        if (!portalRoleRepository.existsByAccountIdAndPortalRole(accountId, role)) {
            AccountPortalRole link = new AccountPortalRole();
            link.setAccountId(accountId);
            link.setPortalRole(role);
            portalRoleRepository.save(link);
            log.info("Granted {} to account {}", role, accountId);
        }
    }
}
