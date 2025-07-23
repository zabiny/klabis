package club.klabis.domain.members;

import club.klabis.domain.members.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.domain.members.forms.EditOwnMemberInfoForm;
import club.klabis.domain.members.forms.MemberEditForm;
import jakarta.validation.Valid;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Validated
@Service
public interface MemberService {
    List<Member> findAll(boolean includeSuspended);

    Optional<Member> findById(Member.Id memberId);

    RegistrationNumber suggestRegistrationNumber(LocalDate dateOfBirth, Sex sex);

    Optional<MembershipSuspensionInfo> getSuspensionInfoForMember(Member.Id memberId);

    void suspendMembershipForMember(Member.Id memberId, boolean forceSuspension);

    Member editMember(Member.Id memberId, @Valid MemberEditForm editForm);

    EditAnotherMemberInfoByAdminForm getEditAnotherMemberForm(Member.Id memberId);

    Member editMember(Member.Id memberId, @Valid EditAnotherMemberInfoByAdminForm form);

    EditOwnMemberInfoForm getEditOwnMemberInfoForm(Member.Id memberId);

    Member editMember(Member.Id memberId, @Valid EditOwnMemberInfoForm form);
}
