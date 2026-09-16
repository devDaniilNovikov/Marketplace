package dn.marketplace.account.api.mapper;

import dn.marketplace.account.api.dto.AccountProfileResponse;
import dn.marketplace.account.api.dto.AccountResponse;
import dn.marketplace.account.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

    @Mapping(target = "deleted", expression = "java(entity.getDeletedAt() != null)")
    AccountResponse toResponse(AccountEntity entity);

    @Mapping(target = "deleted", expression = "java(entity.getDeletedAt() != null)")
    AccountProfileResponse toProfile(AccountEntity entity);
}
