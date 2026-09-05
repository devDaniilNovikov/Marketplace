package dn.marketplace.account.api.dto;


import dn.marketplace.account.api.AccountEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Map;


@Builder
public record AccountMapResponse(Map<String, List<AccountEntity>> accounts) {
}
