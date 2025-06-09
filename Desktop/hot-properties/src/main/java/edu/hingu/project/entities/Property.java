package edu.hingu.project.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    private String location;

    private double price;
    private int size;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images;

    private String imageFolder;

    @Transient
    private String tempImagePath;

    @Transient
    private List<String> imageUrls;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Favorite> favorites = new ArrayList<>();


    public List<Favorite> getFavorites() {
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        return favorites;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<PropertyImage> getImages() { return images; }
    public void setImages(List<PropertyImage> images) { this.images = images; }

    public String getImageFolder() { return imageFolder; }
    public void setImageFolder(String imageFolder) { this.imageFolder = imageFolder; }

    public String getTempImagePath() { return tempImagePath; }
    public void setTempImagePath(String tempImagePath) { this.tempImagePath = tempImagePath; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getThumbnailPath() {
        if (imageFolder != null && !imageFolder.isBlank()) {
            try {
                java.nio.file.Path folderPath = java.nio.file.Paths.get("src/main/resources/static/images/property-images/", imageFolder);
                return java.nio.file.Files.list(folderPath)
                        .findFirst()
                        .map(path -> "/images/property-images/" + imageFolder + "/" + path.getFileName().toString())
                        .orElse("/images/placeholder.jpg");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "/images/placeholder.jpg";
    }
    

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Transient
    private int favoriteCount;

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }


}
