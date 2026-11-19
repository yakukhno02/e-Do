package com.edo.repository;

import com.edo.entity.Room;
import com.edo.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
    List<Room> findAllByStatusIn(Collection<RoomStatus> statuses);
    List<Room> findAllByCreatorId(UUID creatorId);
    void deleteAllByCreatorId(UUID creatorId);
}
