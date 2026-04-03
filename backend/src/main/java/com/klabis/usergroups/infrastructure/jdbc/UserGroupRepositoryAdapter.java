package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SecondaryAdapter
@Repository
class UserGroupRepositoryAdapter implements UserGroupRepository {

    private final UserGroupJdbcRepository jdbcRepository;
    private final NamedParameterJdbcTemplate namedJdbc;

    UserGroupRepositoryAdapter(UserGroupJdbcRepository jdbcRepository, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcRepository = jdbcRepository;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public UserGroup save(UserGroup userGroup) {
        return jdbcRepository.save(UserGroupMemento.from(userGroup)).toUserGroup();
    }

    @Override
    public Optional<UserGroup> findById(UserGroupId id) {
        return jdbcRepository.findById(id.uuid()).map(UserGroupMemento::toUserGroup);
    }

    @Override
    public List<UserGroup> findAll(GroupFilter filter) {
        FilterQuery query = buildFilterQuery(filter);
        List<UUID> ids = namedJdbc.queryForList(query.sql(), query.params(), UUID.class);
        return ids.stream()
                .map(jdbcRepository::findById)
                .flatMap(Optional::stream)
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public Optional<UserGroup> findOne(GroupFilter filter) {
        return findAll(filter).stream().findFirst();
    }

    private FilterQuery buildFilterQuery(GroupFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT ug.id FROM user_groups ug");
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (filter.owner() != null) {
            sql.append(" JOIN user_group_owners ugo ON ug.id = ugo.user_group_id");
            conditions.add("ugo.member_id = :ownerId");
            params.addValue("ownerId", filter.owner().uuid());
        }

        if (filter.member() != null) {
            sql.append(" JOIN user_group_members ugm ON ug.id = ugm.user_group_id");
            conditions.add("ugm.member_id = :memberId");
            params.addValue("memberId", filter.member().uuid());
        }

        if (filter.pendingInvitationForMember() != null) {
            sql.append(" JOIN invitations inv ON ug.id = inv.group_id");
            conditions.add("inv.invited_member_id = :invitedMemberId AND inv.status = 'PENDING'");
            params.addValue("invitedMemberId", filter.pendingInvitationForMember().uuid());
        }

        if (filter.type() != null) {
            conditions.add("ug.type = :type");
            params.addValue("type", filter.type().name());
        }

        if (filter.id() != null) {
            conditions.add("ug.id = :id");
            params.addValue("id", filter.id().uuid());
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        return new FilterQuery(sql.toString(), params);
    }

    private record FilterQuery(String sql, MapSqlParameterSource params) {}

    @Override
    public List<UserGroup> findAllByMember(MemberId memberId) {
        return jdbcRepository.findAllByMemberId(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<UserGroup> findAllByOwner(MemberId memberId) {
        return jdbcRepository.findAllByOwnerId(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<UserGroup> findAllWithPendingInvitationForMember(MemberId memberId) {
        return jdbcRepository.findAllWithPendingInvitationForMember(memberId.uuid()).stream()
                .map(UserGroupMemento::toUserGroup)
                .toList();
    }

    @Override
    public List<TrainingGroup> findAllTrainingGroups() {
        return jdbcRepository.findAllTrainingGroups().stream()
                .map(m -> (TrainingGroup) m.toUserGroup())
                .toList();
    }

    @Override
    public List<FamilyGroup> findAllFamilyGroups() {
        return jdbcRepository.findAllFamilyGroups().stream()
                .map(m -> (FamilyGroup) m.toUserGroup())
                .toList();
    }

    @Override
    public Optional<FamilyGroup> findFamilyGroupByMember(MemberId memberId) {
        return jdbcRepository.findFamilyGroupsByMemberId(memberId.uuid()).stream()
                .findFirst()
                .map(m -> (FamilyGroup) m.toUserGroup());
    }

    @Override
    public void delete(UserGroupId id) {
        jdbcRepository.deleteById(id.uuid());
    }
}
