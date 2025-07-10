package edu.hingu.project.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users") 
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private User agent;

    @OneToMany(mappedBy = "agent")
    @JsonIgnore
    private List<User> clients = new ArrayList<>();

    @Column
    private String profilePicture;

    @Column
    private boolean enabled;

    public User() {}

    public User(String username, String password, String firstName, String lastName,
                String email, Set<Role> roles, String profilePicture) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
        this.profilePicture = profilePicture;
    }

    public Long getId() { return id; }

    public String getName() {
        return firstName + " " + lastName;
    }
    

    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public Set<Role> getRoles() { return roles; }

    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getProfilePicture() { return profilePicture; }

    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public User getAgent() { return agent; }

    public void setAgent(User agent) { this.agent = agent; }

    public List<User> getClients() { return clients; }

    public void setClients(List<User> clients) { this.clients = clients; }

    public void addClient(User u1) {
        this.clients.add(u1);
        u1.setAgent(this);
    }

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<Favorite> favorites = new ArrayList<>();

        public List<Favorite> getFavorites() {
            return favorites;
        }

        public void setFavorites(List<Favorite> favorites) {
            this.favorites = favorites;
        }

        @OneToMany(mappedBy = "user")
        private List<PredictionHistory> predictionHistory;


}    
