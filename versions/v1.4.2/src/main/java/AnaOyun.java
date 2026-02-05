import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import java.util.ArrayList;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class AnaOyun {
    private static final String VERSION = "v1.4.2";

    private static Vector3f pozisyon = new Vector3f(0, 5, 10);
    private static float yatayBakis = 0.0f, dikeyBakis = 0.0f;
    private static double sonMouseX = 500, sonMouseY = 400;

    private static float dikeyHiz = 0.0f;
    private static final float YERCEKIMI = -25.0f;
    private static final float ZIPLAMA_GUCU = 10.0f;
    private static boolean yerdemi = false;
    private static double sonKareZamani = 0;

    private static ArrayList<Vector3f> bloklar = new ArrayList<>();
    private static int seciliSlot = 0;
    private static boolean solBasili = false, sagBasili = false;

    public static void main(String[] args) {
        if (!glfwInit()) throw new IllegalStateException("GLFW hatası!");
        long pencere = glfwCreateWindow(1000, 800, "Java Minecraft " + VERSION, 0, 0);
        glfwMakeContextCurrent(pencere);
        GL.createCapabilities();
        glfwSetInputMode(pencere, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glEnable(GL_DEPTH_TEST);

        // Zemin oluşturma
        for(int x = -10; x <= 10; x++) {
            for(int z = -10; z <= 10; z++) {
                bloklar.add(new Vector3f(x * 2.0f, -1.0f, z * 2.0f));
            }
        }

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspect = 1000.0f / 800.0f;
        glFrustum(-aspect * 0.1, aspect * 0.1, -0.1, 0.1, 0.1, 100.0);
        glMatrixMode(GL_MODELVIEW);

        sonKareZamani = glfwGetTime();

        while (!glfwWindowShouldClose(pencere)) {
            double suankiZaman = glfwGetTime();
            float dt = (float)(suankiZaman - sonKareZamani);
            sonKareZamani = suankiZaman;
            if (dt > 0.05f) dt = 0.05f;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearColor(0.5f, 0.8f, 1.0f, 1.0f);

            kontrolleriGuncelle(pencere, dt);
            fizikUygula(dt);

            glLoadIdentity();
            glRotatef(dikeyBakis, 1, 0, 0);
            glRotatef(yatayBakis, 0, 1, 0);
            glTranslatef(-pozisyon.x, -(pozisyon.y + 1.7f), -pozisyon.z);

            for (Vector3f b : bloklar) blokCiz(b.x, b.y, b.z);
            guiCiz();

            glfwSwapBuffers(pencere);
            glfwPollEvents();
        }
        glfwTerminate();
    }

    // --- YENİ: ÇARPIŞMA KONTROLÜ (AABB) ---
    private static boolean carpismaVarmi(float yeniX, float yeniY, float yeniZ) {
        // Karakterin genişliği (0.6) ve boyu (1.8)
        float genislik = 0.3f;
        float boy = 1.8f;

        for (Vector3f b : bloklar) {
            // Blok sınırları: x-1 ile x+1 (Çünkü blok boyutu 2 birim)
            if (yeniX + genislik > b.x - 1 && yeniX - genislik < b.x + 1 &&
                    yeniY + boy > b.y - 1 && yeniY < b.y + 1 &&
                    yeniZ + genislik > b.z - 1 && yeniZ - genislik < b.z + 1) {
                return true; // Çarpışma var!
            }
        }
        return false;
    }

    private static void fizikUygula(float dt) {
        dikeyHiz += YERCEKIMI * dt;
        float hareketY = dikeyHiz * dt;

        // Y ekseninde çarpışma kontrolü
        if (!carpismaVarmi(pozisyon.x, pozisyon.y + hareketY, pozisyon.z)) {
            pozisyon.y += hareketY;
            yerdemi = false;
        } else {
            if (dikeyHiz < 0) yerdemi = true;
            dikeyHiz = 0;
        }
    }

    private static void kontrolleriGuncelle(long pencere, float dt) {
        if (glfwGetKey(pencere, GLFW_KEY_ESCAPE) == GLFW_PRESS) glfwSetWindowShouldClose(pencere, true);

        float hiz = 10.0f * dt;
        float rad = (float) Math.toRadians(yatayBakis - 90);

        float dx = 0, dz = 0;
        if (glfwGetKey(pencere, GLFW_KEY_W) == GLFW_PRESS) { dx += Math.cos(rad) * hiz; dz += Math.sin(rad) * hiz; }
        if (glfwGetKey(pencere, GLFW_KEY_S) == GLFW_PRESS) { dx -= Math.cos(rad) * hiz; dz -= Math.sin(rad) * hiz; }
        if (glfwGetKey(pencere, GLFW_KEY_A) == GLFW_PRESS) { dx -= Math.cos(rad + Math.PI/2) * hiz; dz -= Math.sin(rad + Math.PI/2) * hiz; }
        if (glfwGetKey(pencere, GLFW_KEY_D) == GLFW_PRESS) { dx += Math.cos(rad + Math.PI/2) * hiz; dz += Math.sin(rad + Math.PI/2) * hiz; }

        // X ve Z eksenlerinde ayrı ayrı çarpışma kontrolü yapıyoruz (Duvara sürtünme için)
        if (!carpismaVarmi(pozisyon.x + dx, pozisyon.y, pozisyon.z)) pozisyon.x += dx;
        if (!carpismaVarmi(pozisyon.x, pozisyon.y, pozisyon.z + dz)) pozisyon.z += dz;

        if (glfwGetKey(pencere, GLFW_KEY_SPACE) == GLFW_PRESS && yerdemi) {
            dikeyHiz = ZIPLAMA_GUCU;
            yerdemi = false;
        }

        // Raycast ve Diğerleri... (v1.4.1 ile aynı)
        if (glfwGetMouseButton(pencere, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS && !solBasili) {
            Vector3f hedef = raycast(false);
            if (hedef != null) bloklar.remove(hedef);
            solBasili = true;
        } else if (glfwGetMouseButton(pencere, GLFW_MOUSE_BUTTON_LEFT) == GLFW_RELEASE) solBasili = false;

        if (glfwGetMouseButton(pencere, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS && !sagBasili) {
            Vector3f yeni = raycast(true);
            if (yeni != null) bloklar.add(yeni);
            sagBasili = true;
        } else if (glfwGetMouseButton(pencere, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_RELEASE) sagBasili = false;

        double[] x = new double[1], y = new double[1];
        glfwGetCursorPos(pencere, x, y);
        yatayBakis += (float)(x[0] - sonMouseX) * 0.15f;
        dikeyBakis += (float)(y[0] - sonMouseY) * 0.15f;
        if (dikeyBakis > 89.0f) dikeyBakis = 89.0f;
        if (dikeyBakis < -89.0f) dikeyBakis = -89.0f;
        sonMouseX = x[0]; sonMouseY = y[0];
    }

    private static Vector3f raycast(boolean koymaModu) {
        float radYatay = (float) Math.toRadians(yatayBakis - 90);
        float radDikey = (float) Math.toRadians(-dikeyBakis);
        float dirX = (float) (Math.cos(radYatay) * Math.cos(radDikey));
        float dirY = (float) Math.sin(radDikey);
        float dirZ = (float) (Math.sin(radYatay) * Math.cos(radDikey));
        Vector3f sonBos = null;
        for (float i = 0; i < 8; i += 0.05f) {
            float px = pozisyon.x + dirX * i;
            float py = (pozisyon.y + 1.7f) + dirY * i;
            float pz = pozisyon.z + dirZ * i;
            for (Vector3f b : bloklar) {
                if (px > b.x - 1 && px < b.x + 1 && py > b.y - 1 && py < b.y + 1 && pz > b.z - 1 && pz < b.z + 1) {
                    if (koymaModu) return sonBos;
                    return b;
                }
            }
            sonBos = new Vector3f(Math.round(px / 2.0f) * 2.0f, Math.round(py / 2.0f) * 2.0f, Math.round(pz / 2.0f) * 2.0f);
        }
        return null;
    }

    private static void guiCiz() {
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        glOrtho(0, 1000, 800, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        glColor3f(1, 1, 1);
        glBegin(GL_LINES); glVertex2f(490, 400); glVertex2f(510, 400); glVertex2f(500, 390); glVertex2f(500, 410); glEnd();
        glEnable(GL_DEPTH_TEST); glPopMatrix(); glMatrixMode(GL_PROJECTION); glPopMatrix(); glMatrixMode(GL_MODELVIEW);
    }

    private static void blokCiz(float x, float y, float z) {
        glPushMatrix(); glTranslatef(x, y, z); glScalef(0.98f, 0.98f, 0.98f);
        glBegin(GL_QUADS);
        glColor3f(0.5f, 0.3f, 0.1f); glVertex3f(-1,-1,1); glVertex3f(1,-1,1); glVertex3f(1,1,1); glVertex3f(-1,1,1);
        glVertex3f(-1,-1,-1); glVertex3f(-1,1,-1); glVertex3f(1,1,-1); glVertex3f(1,-1,-1);
        glColor3f(0.2f, 0.8f, 0.2f); glVertex3f(-1,1,-1); glVertex3f(-1,1,1); glVertex3f(1,1,1); glVertex3f(1,1,-1);
        glColor3f(0.4f, 0.2f, 0.1f); glVertex3f(1,-1,-1); glVertex3f(1,1,-1); glVertex3f(1,1,1); glVertex3f(1,-1,1);
        glVertex3f(-1,-1,-1); glVertex3f(-1,-1,1); glVertex3f(-1,1,1); glVertex3f(-1,1,-1);
        glColor3f(0.3f, 0.2f, 0.1f); glVertex3f(-1,-1,-1); glVertex3f(1,-1,-1); glVertex3f(1,-1,1); glVertex3f(-1,-1,1);
        glEnd(); glPopMatrix();
    }
}