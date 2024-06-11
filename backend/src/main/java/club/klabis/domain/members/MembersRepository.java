package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

@Repository
public interface MembersRepository extends ListCrudRepository<Member, Integer> {

    List<Member> findMembersByBirthYearAndSex(int birthYear, Sex sex);

}
