import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

public class Hitori extends Application {
    //网格数
    public static final int grids = 8;
    //方格宽度
    private int gridW=40;
    private int gridH=40;
    //画布宽度
    private int canvasW=grids*gridW;
    private int canvasH=grids*gridH;
    //画布偏移
    private int canvasX=40;
    private int canvasY=40;
    //棋盘
    private Board board;

    private Double fontSize = 36.0;
    private Double fontY = 6.0 ;//偏移量


    Canvas canvas;//画布
    Button reset;//复原按钮
    Label info;//提示标签

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Hitori Puzzle");
        Group root = new Group();
        initUI();

        board = initBoard();
        drawShapes(canvas.getGraphicsContext2D());

        root.getChildren().add(canvas);
        root.getChildren().add(reset);
        root.getChildren().add(info);

        primaryStage.setScene(new Scene(root, canvasW+2*canvasX, canvasH + 4*canvasY));
        primaryStage.show();
    }

    private void initUI() {
        canvas = new Canvas(canvasW, canvasH);
        canvas.setTranslateX(canvasX);
        canvas.setTranslateY(canvasY);

        //左键、右键事件
        canvas.setOnMouseClicked(event -> {
            Grad grad = doubleToInt(event.getX(),event.getY());
            if (event.getButton().name().equals("PRIMARY"))//左键
                leftClick(grad);
            else if (event.getButton().name().equals("SECONDARY"))//右键
                rightClick(grad);
        });

        reset = new Button("reset");
        setButton(reset);
        info = new Label("welcome to solve Hitori Puzzle!");
        setLabel(info);

    }

    private void setLabel(Label info) {
        info.setTranslateX(0);
        info.setTranslateY(canvasY * 1.1+ canvasH);
        info.setPrefWidth(canvasW+2*canvasX);
        info.setPrefHeight(gridW);
        info.setAlignment(Pos.CENTER);
        info.setFont(new Font("",fontSize/3));
    }

    private void setButton(Button button) {
        button.setTranslateX(canvasX * 2);
        button.setTranslateY(canvasY * 2.2+ canvasH);
        button.setPrefWidth(gridW * (grids - 2));
        button.setPrefHeight(gridW);
        //重置事件
        button.setOnMouseClicked(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("confirm Dialog");
            alert.setHeaderText("Are you sure to reset puzzle");
            Optional<ButtonType> type = alert.showAndWait();
            if (type.get().getButtonData() == ButtonBar.ButtonData.OK_DONE){
                //确定
                board.resetBlack();
                drawShapes(canvas.getGraphicsContext2D());
                info.setText(" ");
            }//取消

        });
    }

    private Grad doubleToInt(double x, double y) {
        int a = (int)x/gridW;
        int b = (int)y/gridH;
        return new Grad(a,b);
    }

    private void rightClick(Grad grad) {
        if (board.getBlack(grad.getX(),grad.getY())){
            board.setBlack(grad.getX(),grad.getY(),false);
            //重新绘制
            drawShapes(canvas.getGraphicsContext2D());
            confirm();
        }
    }


    private void leftClick(Grad grad) {
        if (!board.getBlack(grad.getX(),grad.getY())){
            board.setBlack(grad.getX(),grad.getY(),true);
            blackGrid(grad);
            confirm();
        }
    }
    //验证是否满足条件
    private boolean confirm() {
        //测试黑块是否有连续
        if (!confirmBlack()){
            warning("two continue black cells！");
            return false;
        }
        //测试白块是否单连通
        if (!confirmWhite()){
            warning("white cell isn't in one component");
            return false;
        }

        if (confirmRepeat()){
            success();
            return true;
        }
        info.setText(" ");
        return false;
    }

    private void success() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("infomation Dialog");
        alert.setHeaderText("congratulation!");
        alert.setContentText("you have solved this puzzle!");
        alert.showAndWait();
        info.setTextFill(Color.GREEN);
        info.setText("congratulation! you have solved this puzzle!");

    }

    private void warning(String str) {
        info.setTextFill(Color.RED);
        info.setText(str);
    }

    //每行数字是否重复
    private boolean confirmRepeat() {
        for (int y = 0; y < grids; y++) {
            boolean[] row = new boolean[10];
            for (int x = 0; x < grids; x++) {
                if (!board.getBlack(x,y)){
                    if (row[board.getNum(x,y)]){
                        //该行有重复
                        return false;
                    }
                    else{
                        row[board.getNum(x,y)] = true;
                    }
                }
            }
        }
        for (int x = 0; x < grids; x++) {
            boolean[] col = new boolean[10];
            for (int y = 0; y < grids; y++) {
                if (!board.getBlack(x,y)){
                    if (col[board.getNum(x,y)]){
                        //该列有重复
                        return false;
                    }
                    else{
                        col[board.getNum(x,y)] = true;
                    }
                }
            }
        }
        //验证通过
        return true;
    }

    //单连通测试
    private boolean confirmWhite() {
        boolean[][] blackTmp = board.getBlack();
        //从(0,0)或(0,1)出发检查联通性
        int x=0;
        if (blackTmp[0][0]){
            x = 1;
        }
        Queue<Grad> queue = new ArrayDeque<>();
        queue.add(new Grad(x,0));
        blackTmp[x][0]=true;
        while (!queue.isEmpty()){
            Grad grad = queue.poll();
            if (grad.getX()>0){
                if (!blackTmp[grad.getX()-1][grad.getY()]){
                    queue.add(new Grad(grad.getX()-1,grad.getY()));
                    blackTmp[grad.getX()-1][grad.getY()]=true;
                }
            }
            if (grad.getY()>0){
                if (!blackTmp[grad.getX()][grad.getY()-1]){
                    queue.add(new Grad(grad.getX(),grad.getY()-1));
                    blackTmp[grad.getX()][grad.getY()-1]=true;
                }
            }
            if (grad.getX()<grids-1){
                if (!blackTmp[grad.getX()+1][grad.getY()]){
                    queue.add(new Grad(grad.getX()+1,grad.getY()));
                    blackTmp[grad.getX()+1][grad.getY()]=true;
                }
            }
            if (grad.getY()<grids-1){
                if (!blackTmp[grad.getX()][grad.getY()+1]){
                    queue.add(new Grad(grad.getX(),grad.getY()+1));
                    blackTmp[grad.getX()][grad.getY()+1]=true;
                }
            }
        }
        for (int i = 0; i < grids; i++) {
            for (int j = 0; j < grids; j++) {
                if (!blackTmp[i][j]){
                    //验证不通过
                    return false;
                }
            }
        }

        return true;
    }

    //连续黑块测试
    private boolean confirmBlack() {
        for (int x = 0; x < grids-1; x++) {
            for (int y = 0; y <grids-1; y++) {
                if (board.getBlack(x,y)){
                    if (board.getBlack(x+1,y) || board.getBlack(x,y+1)){
                        return false;
                    }
                }
            }
        }
		for (int i = 0; i <grids-1; i++) {
			if (board.getBlack(grids-1,i)){
				if (board.getBlack(grids-1,i+1)){
					return false;
				}
			}
			if (board.getBlack(i,grids-1)){
				if (board.getBlack(i+1,grids-1)){
					return false;
				}
			}
		}
        return true;
    }

    private void blackGrid(Grad grad) {
        canvas.getGraphicsContext2D().fillRect(grad.getX()*gridW,grad.getY()*gridH,gridW,gridH);
    }

    private Board initBoard() {

        int[][] nums={{4,8,1,6,3,2,5,7},
                {3,6,7,2,1,6,5,4},
                {2,3,4,8,2,8,6,1},
                {4,1,6,5,7,7,3,5},
                {7,2,3,1,8,5,1,2},
                {3,5,6,7,3,1,8,4},
                {6,4,2,3,5,4,7,8},
                {8,7,1,4,2,3,5,6}
        };
/*        int[][] nums={{4,8,1,6,3,2,5,7,5,7},
                {3,6,7,2,1,6,5,4,5,7},
                {2,3,4,8,2,8,6,1,5,7},
                {4,1,6,5,7,7,3,5,5,7},
                {7,2,3,1,8,5,1,2,5,7},
                {3,5,6,7,3,1,8,4,5,7},
                {6,4,2,3,5,4,7,8,5,7},
                {8,7,1,4,2,3,5,6,5,7},
                {8,7,1,4,2,3,5,6,5,7},
                {8,7,1,4,2,3,5,6,5,7}
        };*/

        BufferedReader br = null;
        //尝试从文件中读取数据
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("data")));
            String str = null;
            int y = 0;
            while ((str=br.readLine())!=null){
                String[] s = str.split(" ");
                for (int x = 0; x < s.length; x++) {
                    int t = Integer.parseInt(s[x]);
                    nums[y][x] = t;
                }
                y++;
            }
            info.setText("success reading data from file！");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (br != null)
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        Board board = new Board(nums,grids);
        return board;
    }

    private void drawShapes(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(new Font("", fontSize));
        drawGrids(gc);
        drawNums(gc);

    }

    //绘制数字
    private void drawNums(GraphicsContext gc) {
        for (int y = 0; y < grids; y++) {
            for (int x = 0; x < grids; x++) {
                gc.fillText(String.valueOf(board.getNums()[y][x]),x*gridW + gridW/2,(y+1)*gridH - fontY);
                if (board.getBlack(x,y)){
                    gc.fillRect(x*gridW,y*gridH,gridW,gridH);
                }
            }
        }
       // gc.fillText("0",20,35);
    }

    //绘制网格
    private void drawGrids(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,canvasW,canvasH);
        gc.setFill(Color.BLACK);
        for (int i = 0; i <= grids; i++) {
            //画竖线
            gc.strokeLine(i*gridW,0,i*gridW,canvasH);
            //画横线
            gc.strokeLine(0,i*gridH,canvasW,i*gridH);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}

class Grad{
    private int x;
    private int y;

    public Grad(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}

class Board {
    private final int[][] nums;//存棋盘数字
    private final boolean[][] black;//存黑块,黑为true，否则为fasle
    private final int grads;

    public Board(int[][] nums, int grids) {
        this.nums = nums;
        black = new boolean[grids][grids];
        this.grads = grids;
    }

    public int[][] getNums() {
        return nums;
    }
    public int getNum(int x,int y) {
        return nums[y][x];
    }

    public boolean[][] getBlack() {
        boolean[][] tmp = new boolean[grads][grads];
        for (int i = 0; i < grads; i++) {
            tmp[i] = black[i].clone();
        }
        return tmp;
    }

    public boolean getBlack(int x, int y) {
        return black[y][x];
    }

    public void setBlack(int x,int y,boolean flag) {
        this.black[y][x]=flag;
    }

    public void resetBlack() {
        for (int y=0;y<grads;y++){
            for (int x=0;x<grads;x++){
                black[y][x]=false;
            }
        }
    }
}
