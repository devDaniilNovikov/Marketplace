package dn.marketplace.account.api.mapper;

import dn.marketplace.account.api.dto.AccountProfileResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

    // deleted берётся из AccountEntity.isDeleted()
    AccountResponse toResponse(AccountEntity entity);

    AccountProfileResponse toProfile(AccountEntity entity);
}
