package com.example.app.api.syncdata;

import com.example.app.domain.sync.ExternalDataSync;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private static final int EXPORT_LIMIT = 10_000;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {
            "ID", "외부 참조 ID", "상태", "오류 메시지", "재시도 횟수", "동기화 시각", "생성 시각"
    };

    private final SyncDataQueryService syncDataQueryService;

    public void exportSyncData(
            Long systemId,
            ExternalDataSync.SyncStatus status,
            String refId,
            OutputStream out) throws IOException {

        List<ExternalDataSync> items = syncDataQueryService.findAllForExport(systemId, status, refId);
        List<ExternalDataSync> limited = items.size() > EXPORT_LIMIT ? items.subList(0, EXPORT_LIMIT) : items;

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("동기화 데이터");
            CellStyle headerStyle = buildHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ExternalDataSync item : limited) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getId());
                row.createCell(1).setCellValue(item.getExternalRefId());
                row.createCell(2).setCellValue(item.getStatus().name());
                row.createCell(3).setCellValue(item.getErrorMessage() != null ? item.getErrorMessage() : "");
                row.createCell(4).setCellValue(item.getRetryCount());
                row.createCell(5).setCellValue(fmt(item.getSyncedAt()));
                row.createCell(6).setCellValue(fmt(item.getCreatedAt()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
        }
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(FMT) : "";
    }
}
