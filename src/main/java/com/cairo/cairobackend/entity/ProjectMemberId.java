package com.cairo.cairobackend.entity;

import lombok.*;

import java.io.Serializable;


//JPA requires a separate class for composite primary keys.
//This represents the (project_id, user_id) pair that makes
//up the primary key of the project_members table.
//Must implement Serializable and override equals/hashCode -
//Lombok's @EqualsAndHashCode handles that for us.


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProjectMemberId implements Serializable {
    private Long projectId;
    private Long userId;
}
