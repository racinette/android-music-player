package playlist;

public class MainPlaylist extends AbstractPlaylist {
    public MainPlaylist(){
        super("All Songs", false);
    }

    public void setFlag(int index, boolean flag){
        getTrack(index).setFlag(flag);
    }

    public boolean getFlag(int index){
        return getTrack(index).getFlag();
    }

}
