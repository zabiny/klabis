package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@Repository
@NoRepositoryBean
public interface MembersRepository extends ListCrudRepository<Member, Integer> {

    List<Member> findMembersByBirthYearAndSex(int birthYear, Sex sex);

}
