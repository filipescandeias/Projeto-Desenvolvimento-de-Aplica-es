package VIEWS;

import CONTROLLERS.HomeController;
import MODELS.CLASS.Bilhete;
import MODELS.CLASS.Evento;
import MODELS.CLASS.EventoRecurso;
import MODELS.CLASS.NaoConsumivel;
import MODELS.CLASS.Recurso;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author gonca
 */
public class PanelFinanceiro extends javax.swing.JPanel {

    private PaginaInicial janelaPrincipal;
    private HomeController controller;
    
    // Rutas de iconos
    private final String ICON_ERRO = "src/main/java/Recursos/erro.png";
    private final String ICON_AVISO = "src/main/java/Recursos/aviso.png";
    private final String ICON_INFO = "src/main/java/Recursos/info.png";

    /**
     * Creates new form PanelFinanceiro
     */
    public PanelFinanceiro(PaginaInicial janelaPrincipal) {
        this.janelaPrincipal = janelaPrincipal;
        this.controller = new HomeController();
        initComponents();

        tabelaReceitas.getTableHeader().setResizingAllowed(false);
        tabelaReceitas.getTableHeader().setReorderingAllowed(false);
        tabelaReceitas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        tabelaDespesas.getTableHeader().setResizingAllowed(false);
        tabelaDespesas.getTableHeader().setReorderingAllowed(false);
        tabelaDespesas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // Modelo personalizado para Despesas
        String[] colunasDespesas = {"Id", "Nome", "Categoria", "Data", "Qtd", "Preco", "Total"};
        DefaultTableModel modeloDespesas = new DefaultTableModel(colunasDespesas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaDespesas.setModel(modeloDespesas);

        DefaultTableModel modelo = (DefaultTableModel) tabelaReceitas.getModel();
        modelo.setRowCount(0);

        // --- CARGAR RECEITAS ---
        List<EventoRecurso> lista = controller.listarEventoRecursoNaoConsumiveis();
        float soma = 0;

        for (EventoRecurso er : lista) {
            int idEvento = er.getIdEvento();
            int idRecurso = er.getIdRecurso();
            String nomeEvento = null, nomeRecurso = null;
            Date data = null;
            float preco = 0;

            Evento evento = controller.buscarEventoPorId(idEvento);
            if (evento != null) {
                nomeEvento = evento.getNome();
                data = evento.getData();
            }

            NaoConsumivel nc = controller.buscarNaoConsumivelPorId(idRecurso);
            if (nc != null) {
                nomeRecurso = nc.getNome() + "(Aluguel)";
                preco = (float) nc.getPrecoAluguer();
            }
            soma += preco;

            modelo.addRow(new Object[]{idEvento, nomeEvento, nomeRecurso, data, preco});
        }

        List<Evento> listaEventosAlugados = controller.listarEventosAlugados();
        for (Evento ev : listaEventosAlugados) {
            soma += ev.getPreco();
            modelo.addRow(new Object[]{ev.getIdEvento(), ev.getNome(), "Evento Alugado", ev.getData(), ev.getPreco()});
        }

        List<Bilhete> listaBilhetes = controller.listarTodosBilhetes();
        for (Bilhete b : listaBilhetes) {
            Evento ev = controller.buscarEventoPorId(b.getIdEvento());
            if (ev != null) {
                soma += b.getPreco();
                modelo.addRow(new Object[]{b.getIdBilhete(), ev.getNome(), "Bilhete", ev.getData(), b.getPreco()});
            }
        }

        List<EventoRecurso> lista4 = controller.listarConsumiveisEventosAlugados();
        for (EventoRecurso er : lista4) {
            Recurso r = controller.buscarPorId(er.getIdRecurso());
            if (r != null) {
                float totalConsumivel = (float) (er.getQuantidade() * r.getPreco());
                soma += totalConsumivel;
                modelo.addRow(new Object[]{er.getIdEvento(), r.getNome(), "Consumo em Aluguer", null, totalConsumivel});
            }
        }

        DecimalFormat df = new DecimalFormat("0.00");
        lblTotalReceitas.setText(df.format(soma) + "€");

        // --- CARGAR DESPESAS ---
        float soma2 = 0;
        modeloDespesas.setRowCount(0);

        List<Recurso> listaDespesas = controller.listarTodosOsRecursosSemConsiderarAtivo();
        for (Recurso r : listaDespesas) {
            String tipo = controller.identificarTipoRecurso(r.getIdRecurso());
            float subtotal = (float) (r.getQuantidade() * r.getPreco());
            soma2 += subtotal;

            modeloDespesas.addRow(new Object[]{
                r.getIdRecurso(), r.getNome(), tipo, null,
                r.getQuantidade(), r.getPreco(), String.format("%.2f €", subtotal)
            });
        }

        List<Evento> listaEventosProprios = controller.listarEventosProprios();
        for (Evento ev : listaEventosProprios) {
            soma2 += ev.getPreco();
            modeloDespesas.addRow(new Object[]{
                ev.getIdEvento(), ev.getNome(), "Evento Próprio", ev.getData(),
                1, ev.getPreco(), String.format("%.2f €", ev.getPreco())
            });
        }

        lvlTotalDespesas.setText(df.format(soma2) + "€");
        jLabel4.setText(df.format(soma - soma2) + "€");
    }

    /**
     * Método auxiliar para mostrar los pop-ups personalizados
     */
    private void exibirMensagem(String titulo, String mensagem, String caminhoImagem) {
        java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
        java.awt.Frame parentFrame = null;
        if (parentWindow instanceof java.awt.Frame) {
            parentFrame = (java.awt.Frame) parentWindow;
        }

        PaginaDialogo dialogo = new PaginaDialogo(parentFrame, true);
        dialogo.setMensagem(mensagem, titulo, caminhoImagem);
        dialogo.setVisible(true);
    }

    private void filtrarPorIntervaloDatas() {
        Date dataInicioRaw = txtDataInicio.getDate();
        Date dataFimRaw = txtDataFim.getDate();

        // Validación: Data Fim no puede ser menor que Data Inicio
        if (dataInicioRaw != null && dataFimRaw != null) {
            if (dataFimRaw.before(dataInicioRaw)) {
                exibirMensagem("Intervalo Inválido", "A 'Data Fim' não pode ser anterior à 'Data Início'.", ICON_AVISO);
                return;
            }
        }

        // Ajuste de horas
        final Date dataInicio;
        if (dataInicioRaw != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(dataInicioRaw);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            dataInicio = c.getTime();
        } else {
            dataInicio = null;
        }

        final Date dataFim;
        if (dataFimRaw != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(dataFimRaw);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            c.set(Calendar.MILLISECOND, 999);
            dataFim = c.getTime();
        } else {
            dataFim = null;
        }

        // Filtro
        RowFilter<Object, Object> dateFilter = new RowFilter<Object, Object>() {
            @Override
            public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
                if (dataInicio == null && dataFim == null) {
                    return true;
                }
                int colIndex = 3; // Columna fecha
                Object valor = entry.getValue(colIndex);

                if (valor instanceof Date) {
                    Date dataRow = (Date) valor;
                    boolean despuesInicio = (dataInicio == null) || (!dataRow.before(dataInicio));
                    boolean antesFim = (dataFim == null) || (!dataRow.after(dataFim));
                    return despuesInicio && antesFim;
                }
                return false; 
            }
        };

        DefaultTableModel modelReceitas = (DefaultTableModel) tabelaReceitas.getModel();
        TableRowSorter<DefaultTableModel> sorterReceitas = new TableRowSorter<>(modelReceitas);
        tabelaReceitas.setRowSorter(sorterReceitas);
        sorterReceitas.setRowFilter(dateFilter);

        DefaultTableModel modelDespesas = (DefaultTableModel) tabelaDespesas.getModel();
        TableRowSorter<DefaultTableModel> sorterDespesas = new TableRowSorter<>(modelDespesas);
        tabelaDespesas.setRowSorter(sorterDespesas);
        sorterDespesas.setRowFilter(dateFilter);

        recalcularTotaisVisiveis();
    }

    private void recalcularTotaisVisiveis() {
        DecimalFormat df = new DecimalFormat("0.00");
        float somaReceitas = 0;
        float somaDespesas = 0;

        for (int i = 0; i < tabelaReceitas.getRowCount(); i++) {
            Object valorObj = tabelaReceitas.getValueAt(i, 4);
            if (valorObj instanceof Number) {
                somaReceitas += ((Number) valorObj).floatValue();
            }
        }

        for (int i = 0; i < tabelaDespesas.getRowCount(); i++) {
            Object valorObj = tabelaDespesas.getValueAt(i, 6);

            if (valorObj instanceof Number) {
                somaDespesas += ((Number) valorObj).floatValue();
            } else if (valorObj instanceof String) {
                String s = (String) valorObj;
                s = s.replace("€", "").replace(",", ".").trim();
                try {
                    somaDespesas += Float.parseFloat(s);
                } catch (NumberFormatException ex) {
                }
            }
        }

        lblTotalReceitas.setText(df.format(somaReceitas) + "€");
        lvlTotalDespesas.setText(df.format(somaDespesas) + "€");
        jLabel4.setText(df.format(somaReceitas - somaDespesas) + "€");
    }

    private void exportarParaExcel(javax.swing.JTable table, java.io.File file) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Dados");
            org.apache.poi.ss.usermodel.Row rowCol = sheet.createRow(0);

            for (int i = 0; i < table.getColumnCount(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = rowCol.createCell(i);
                cell.setCellValue(table.getColumnName(i));
            }

            for (int j = 0; j < table.getRowCount(); j++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(j + 1);
                for (int k = 0; k < table.getColumnCount(); k++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(k);
                    if (table.getValueAt(j, k) != null) {
                        cell.setCellValue(table.getValueAt(j, k).toString());
                    }
                }
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                workbook.write(out);
                exibirMensagem("Sucesso", "Exportado com sucesso!", ICON_INFO);
            }
        } catch (Exception e) {
            exibirMensagem("Erro", "Erro ao exportar: " + e.getMessage(), ICON_ERRO);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblTotalReceitas = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lvlTotalDespesas = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabelaReceitas = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabelaDespesas = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        btnExportarRelatorio = new javax.swing.JButton();
        txtDataFim = new com.toedter.calendar.JDateChooser();
        txtDataInicio = new com.toedter.calendar.JDateChooser();

        setBackground(new java.awt.Color(232, 235, 238));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(153, 255, 153));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel1.setText("Total Receitas");

        lblTotalReceitas.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTotalReceitas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTotalReceitas.setText("0.00€");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 82, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(0, 83, Short.MAX_VALUE))
                    .addComponent(lblTotalReceitas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(lblTotalReceitas)
                .addContainerGap(74, Short.MAX_VALUE))
        );

        add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 260, 160));

        jPanel2.setBackground(new java.awt.Color(255, 102, 102));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("Total Despesas");

        lvlTotalDespesas.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lvlTotalDespesas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lvlTotalDespesas.setText("0.00€");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(82, 82, 82)
                .addComponent(jLabel2)
                .addContainerGap(87, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lvlTotalDespesas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(lvlTotalDespesas)
                .addContainerGap(73, Short.MAX_VALUE))
        );

        add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 0, 260, 160));

        jPanel3.setBackground(new java.awt.Color(102, 153, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Saldo Atual");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("0.00€");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(94, 94, 94)
                .addComponent(jLabel3)
                .addContainerGap(97, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addContainerGap(72, Short.MAX_VALUE))
        );

        add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 0, 260, 160));

        tabelaReceitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Id Evento", "Nome", "Tipo(Bilhete/Aluguer)", "Data", "Valor"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tabelaReceitas);

        jTabbedPane1.addTab("Receitas", jScrollPane1);

        tabelaDespesas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Id", "Nome", "Categoria", "Quantidade Comprada", "Preco Unitario", "Custo total"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(tabelaDespesas);

        jTabbedPane1.addTab("Despesas", jScrollPane2);

        add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 230, 780, 260));

        jLabel6.setText("Data Inicio:");
        add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, -1, -1));

        jLabel7.setText("Data Fim:");
        add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 170, -1, -1));

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton1.setText("Filtrar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 190, 100, 35));

        btnExportarRelatorio.setBackground(new java.awt.Color(51, 121, 232));
        btnExportarRelatorio.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnExportarRelatorio.setForeground(new java.awt.Color(255, 255, 255));
        btnExportarRelatorio.setText("Exportar Relatorio");
        btnExportarRelatorio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarRelatorioActionPerformed(evt);
            }
        });
        add(btnExportarRelatorio, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 500, 190, 40));
        add(txtDataFim, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 190, 100, 35));
        add(txtDataInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 190, 100, 35));
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        filtrarPorIntervaloDatas();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btnExportarRelatorioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarRelatorioActionPerformed
       javax.swing.JTable tabelaAtiva;
        String nomeRelatorio;

        if (jTabbedPane1.getSelectedIndex() == 0) {
            tabelaAtiva = tabelaReceitas;
            nomeRelatorio = "Relatorio_Receitas";
        } else {
            tabelaAtiva = tabelaDespesas;
            nomeRelatorio = "Relatorio_Despesas";
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Salvar Relatório");
        chooser.setSelectedFile(new java.io.File(nomeRelatorio + ".xlsx"));

        int userSelection = chooser.showSaveDialog(this);

        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File arquivoParaSalvar = chooser.getSelectedFile();

            String filePath = arquivoParaSalvar.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".xlsx")) {
                arquivoParaSalvar = new java.io.File(filePath + ".xlsx");
            }

            exportarParaExcel(tabelaAtiva, arquivoParaSalvar);
        }
    }//GEN-LAST:event_btnExportarRelatorioActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnExportarRelatorio;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblTotalReceitas;
    private javax.swing.JLabel lvlTotalDespesas;
    private javax.swing.JTable tabelaDespesas;
    private javax.swing.JTable tabelaReceitas;
    private com.toedter.calendar.JDateChooser txtDataFim;
    private com.toedter.calendar.JDateChooser txtDataInicio;
    // End of variables declaration//GEN-END:variables
}
