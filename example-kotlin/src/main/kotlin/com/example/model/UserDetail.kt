package com.example.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class UserDetail(
    @Id
    var name: String = "",
    var gender: String = ""
)
