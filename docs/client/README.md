# Zindigi Corporate Portal — Client Documents

Bank / EMI client pack for **Zindigi Corporate Portal**.

| Document | ID | Markdown | HTML | PDF |
|----------|----|----------|------|-----|
| Business Requirements Document | DFS-CP-BRD-001 | `BRD-Zindigi-Corporate-Portal.md` | `BRD-Zindigi-Corporate-Portal.html` | `pdf/BRD-Zindigi-Corporate-Portal.pdf` |
| Functional Specification Document | DFS-CP-FSD-001 | `FSD-Zindigi-Corporate-Portal.md` | `FSD-Zindigi-Corporate-Portal.html` | `pdf/FSD-Zindigi-Corporate-Portal.pdf` |
| Product Document | DFS-CP-PD-001 | `Product-Document-Zindigi-Corporate-Portal.md` | `Product-Document-Zindigi-Corporate-Portal.html` | `pdf/Product-Document-Zindigi-Corporate-Portal.pdf` |

## Regenerate PDFs

From this folder (PowerShell):

```powershell
.\export-pdfs.ps1
```

Requires Google Chrome. Opens each HTML file headless and writes A4 PDFs under `pdf/`.

## Manual PDF (any browser)

1. Open the `.html` file in Chrome or Edge  
2. Print → **Save as PDF**  
3. Enable **Background graphics** for cover colours and table headers  

## Notes

- Compliance stubs (NADRA BV, automated sanctions) and commercials are marked **TBD**  
- Styling: `assets/client-docs.css`  
