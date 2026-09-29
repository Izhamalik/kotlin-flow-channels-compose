package com.example.flowschannels.data.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val city: String,
)

val SampleUsers: List<User> = listOf(
    User(1, "Ada Lovelace", "ada@flow.dev", "London"),
    User(2, "Grace Hopper", "grace@flow.dev", "New York"),
    User(3, "Alan Turing", "alan@flow.dev", "Cambridge"),
    User(4, "Margaret Hamilton", "margaret@flow.dev", "Boston"),
    User(5, "Barbara Liskov", "barbara@flow.dev", "Boston"),
    User(6, "Donald Knuth", "donald@flow.dev", "Stanford"),
)
