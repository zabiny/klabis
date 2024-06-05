package club.klabis.domain.members;

import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@Repository
@NoRepositoryBean
public interface MembersRepository extends ListCrudRepository<Member, Integer> {

}
