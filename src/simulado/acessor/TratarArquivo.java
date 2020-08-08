package simulado.acessor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

public class TratarArquivo {

  private Connection con;
  private final String NOME_ARQUIVO = "SIMPMP.db";

  public TratarArquivo() {
  }

  public boolean abrirDatabase() {
    try {
      Class.forName("org.hsqldb.jdbcDriver");
      con = DriverManager.getConnection("jdbc:hsqldb:file:" + NOME_ARQUIVO, "sa", "");
      return true;
    } catch (ClassNotFoundException e) {
      JOptionPane.showMessageDialog(null, (new StringBuilder("Erro: ")).append(e.getMessage()).toString(), "Erro", 0);
      return false;
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(null, (new StringBuilder("Erro: ")).append(e.getMessage()).toString(), "Erro", 0);
      return false;
    }
  }

  public boolean criarDatabase() {
    boolean ret = false;
    if (abrirDatabase()) {
      try {
        Statement stm = con.createStatement();
        stm.executeUpdate("DROP TABLE prova IF EXISTS;");
        stm.executeUpdate("CREATE TABLE prova (id CHAR(7) PRIMARY KEY, titulo VARCHAR(60));");
        stm.executeUpdate("DROP TABLE questoes IF EXISTS;");
        stm.executeUpdate(
            "CREATE TABLE questoes (prova CHAR(7), idquestao CHAR(6), pergunta VARCHAR(20000), opcaoA VARCHAR(20000), opcaoB VARCHAR(20000), opcaoC VARCHAR(20000), opcaoD VARCHAR(20000), resposta CHAR(1), area VARCHAR(20), grupo VARCHAR(20), aula INTEGER, PRIMARY KEY (prova, idquestao));");
        stm.close();
        stm = con.createStatement();
        stm.execute("CHECKPOINT;");
        con.commit();
        ret = true;
      } catch (SQLException ex) {
        System.out.println((new StringBuilder("criarDatabase: ")).append(ex.getMessage()).toString());
      }
      fecharDatabase();
    }
    Atributo.titulo = "Importe as Questões da " + Atributo.prova;
    return ret;
  }

  public void fecharDatabase() {
    try {
      con.close();
    } catch (SQLException ex) {
      System.out.println((new StringBuilder("fecharDatabase: ")).append(ex.getMessage()).toString());
    }
  }

  public List<Questao> obterDados(int totalQst) {
    List<Questao> lista = null;
    if (ArquivosTexto.verificarExistencia("SIMPMP.db.properties")) {
      lista = new ArrayList<>();
      if (abrirDatabase()) {
        try {
          Statement stm = con.createStatement();
          ResultSet res = stm.executeQuery(montaSql());
          int addQst = 0;
          while (res.next()) {
            Questao questoes = new Questao(res.getString(1), res.getString(2), res.getString(3), res.getString(4),
                res.getString(5), res.getString(6), res.getString(7).charAt(0), res.getString(8), res.getString(9));
            lista.add(questoes);
            if (totalQst > -1 && ++addQst == totalQst)
              break;
          }
          res.close();
          stm.close();
        } catch (Exception ex) {
          System.out.println((new StringBuilder("obterDados: ")).append(ex.getMessage()).toString());
        }
        fecharDatabase();
      }
    }
    return lista;
  }

  private String montaSql() {
    String monta = "SELECT idquestao, pergunta, opcaoA, opcaoB, opcaoC, opcaoD, resposta, area, grupo FROM questoes WHERE prova = '"
        + Atributo.prova + "'";
    if (Atributo.areaEsc.length() > 0) {
      monta += " AND area = '" + Atributo.areaEsc + "'";
    }
    if (Atributo.grupoEsc.length() > 0) {
      monta += " AND grupo = '" + Atributo.grupoEsc + "'";
    }
    if (Atributo.aula > 0) {
      monta += " AND aula = " + Atributo.aula;
    }
    return (new StringBuilder(String.valueOf(monta))).append(" ORDER BY rand()").toString();
  }

  public boolean limparQuestoes() {
    boolean ret = false;
    if (abrirDatabase()) {
      try {
        Statement stm = con.createStatement();
        stm.executeUpdate("DELETE FROM questoes WHERE prova = '" + Atributo.prova + "'");
        stm.executeUpdate("DELETE FROM prova WHERE id = '" + Atributo.prova + "'");
        stm.close();
        stm = con.createStatement();
        stm.execute("CHECKPOINT;");
        con.commit();
        ret = true;
      } catch (SQLException ex) {
        System.out.println((new StringBuilder("criarDatabase: ")).append(ex.getMessage()).toString());
      }
      fecharDatabase();
    }
    Atributo.titulo = "Importe as Questões da " + Atributo.prova;
    return ret;
  }

  public int totalRegistro() {
    int total = 0;
    if (ArquivosTexto.verificarExistencia("SIMPMP.db.properties")) {
      if (abrirDatabase()) {
        try {
          Statement stm = con.createStatement();
          ResultSet res = stm.executeQuery("SELECT count(*) FROM questoes WHERE prova = '" + Atributo.prova + "'");
          if (res.next()) {
            total = res.getInt(1);
          }
          res.close();
          stm.close();
        } catch (Exception ex) {
          System.out.println((new StringBuilder("totalRegistro: ")).append(ex.getMessage()).toString());
        }
        fecharDatabase();
      }
    }
    return total;
  }

  public void obterTitulo() {
    if (ArquivosTexto.verificarExistencia("SIMPMP.db.properties")) {
      String titulo = "Importe as Questões da " + Atributo.prova;
      if (abrirDatabase()) {
        try {
          Statement stm = con.createStatement();
          ResultSet res = stm.executeQuery("SELECT titulo FROM prova WHERE id = '" + Atributo.prova + "'");
          if (res.next()) {
            titulo = res.getString(1);
          }
          res.close();
          stm.close();
        } catch (Exception ex) {
          System.out.println((new StringBuilder("totalRegistro: ")).append(ex.getMessage()).toString());
        }
        fecharDatabase();
      }
      Atributo.titulo = titulo;
    }
  }

  public int importarDados(String nomArq) {
    int total = 0;
    if (ArquivosTexto.verificarExistencia("SIMPMP.db.properties")) {
      if (abrirDatabase()) {
        try {
          BufferedReader arquivo = new BufferedReader(new FileReader(nomArq));
          // Ler Prova (deve estar na primeira linha)
          String linMnt = arquivo.readLine();
          PreparedStatement pstm = con.prepareStatement("INSERT INTO prova (id, titulo) VALUES (?, ?)");
          StringTokenizer strTok = new StringTokenizer(linMnt, "\253");
          pstm.setString(1, strTok.nextToken());
          pstm.setString(2, strTok.nextToken());
          pstm.executeUpdate();
          // Ler as questões
          pstm = con.prepareStatement(
              "INSERT INTO questoes (prova, idquestao, pergunta, opcaoA, opcaoB, opcaoC, opcaoD, resposta, area, grupo, aula) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

          while ((linMnt = arquivo.readLine()) != null) {
            if (linMnt.charAt(0) != '%') {
              strTok = new StringTokenizer(linMnt, "\253");
              if (strTok.countTokens() == 11) {
                pstm.setString(1, strTok.nextToken());
                pstm.setString(2, strTok.nextToken());
                pstm.setString(3, strTok.nextToken());
                pstm.setString(4, strTok.nextToken());
                pstm.setString(5, strTok.nextToken());
                pstm.setString(6, strTok.nextToken());
                pstm.setString(7, strTok.nextToken());
                pstm.setString(8, strTok.nextToken());
                pstm.setString(9, strTok.nextToken());
                pstm.setString(10, strTok.nextToken());
                pstm.setString(11, strTok.nextToken());
                pstm.executeUpdate();
                total++;
              } else {
                System.out.println("Linha Errada: " + linMnt);
              }
            }
          }
          arquivo.close();
          pstm.close();
          Statement stm = con.createStatement();
          stm.execute("CHECKPOINT;");
          con.commit();
        } catch (Exception ex) {
          System.out.println((new StringBuilder("importarDados: ")).append(ex.getMessage()).toString());
        }
        fecharDatabase();
        obterTitulo();
      }
    }
    return total;
  }

  public int exportarDados(String nomArq) {
    int total = 0;
    if (ArquivosTexto.verificarExistencia("SIMPMP.db.properties")) {
      if (abrirDatabase()) {
        try {
          FileWriter arquivo = new FileWriter(nomArq);
          Statement stm = con.createStatement();
          ResultSet res = stm.executeQuery(
              "SELECT idquestao, pergunta, opcaoA, opcaoB, opcaoC, opcaoD, resposta, area, grupo, aula FROM questoes");
          while (res.next()) {
            arquivo.write(res.getString(1) + "\253" + res.getString(2) + "\253" + res.getString(3) + "\253"
                + res.getString(4) + "\253" + res.getString(5) + "\253" + res.getString(6) + "\253" + res.getString(7)
                + "\253" + res.getString(8) + "\253" + res.getString(9) + "\253" + res.getString(10) + "\n");
          }
          arquivo.close();
          res.close();
          stm.close();
        } catch (Exception ex) {
          System.out.println((new StringBuilder("exportarDados: ")).append(ex.getMessage()).toString());
        }
        fecharDatabase();
      }
    }
    return total;
  }
}
