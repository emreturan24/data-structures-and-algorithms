class Song {
    String name;
    String genre;
    int duration;
    int likes;
    Song next;

    public Song(String name, String genre, int duration, int likes) {
        this.name = name;
        this.genre = genre;
        this.duration = duration;
        this.likes = likes;
        this.next = null;
    }
}