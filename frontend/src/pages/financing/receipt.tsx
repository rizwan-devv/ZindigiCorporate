import { useRef } from 'react'

export type ReceiptData = {
  receiptNo: string
  applicationNo: string
  customerName: string
  productTitle: string
  amount?: number
  fromWallet?: string
  toWallet?: string
  paidAt?: string
}

const money = (value?: number) => value == null ? '—' : `Rs ${Number(value).toLocaleString('en-PK', { maximumFractionDigits: 2 })}`

export function PaymentReceipt({ data }: { data: ReceiptData }) {
  const slip = useRef<HTMLElement>(null)
  const when = data.paidAt ? new Date(data.paidAt).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '—'
  return (
    <div className="slip-wrap">
      <article className="slip" ref={slip}>
        <header>
          <p>Zindigi</p>
          <h3>Payment receipt</h3>
          <span>Paid</span>
        </header>
        <p className="slip-amount">{money(data.amount)}</p>
        <p className="slip-caption">Down payment received</p>
        <dl>
          <div><dt>Receipt no.</dt><dd>{data.receiptNo}</dd></div>
          <div><dt>Application</dt><dd>{data.applicationNo}</dd></div>
          <div><dt>Customer</dt><dd>{data.customerName || '—'}</dd></div>
          <div><dt>Product</dt><dd>{data.productTitle || '—'}</dd></div>
          <div><dt>From wallet</dt><dd>{data.fromWallet || '—'}</dd></div>
          <div><dt>To bank</dt><dd>{data.toWallet || '—'}</dd></div>
          <div><dt>Paid at</dt><dd>{when}</dd></div>
        </dl>
        <footer>This receipt is for the down payment only. The financed amount is not included.</footer>
      </article>
      <div className="actions">
        <button type="button" onClick={() => downloadPng(slip.current, data.receiptNo)}>Download PNG</button>
        <button className="ghost" type="button" onClick={() => downloadPdf(data, when)}>Download PDF</button>
      </div>
    </div>
  )
}

function downloadPng(node: HTMLElement | null, receiptNo: string) {
  if (!node) return
  const canvas = document.createElement('canvas')
  const width = 720
  const height = 980
  canvas.width = width
  canvas.height = height
  const pen = canvas.getContext('2d')
  if (!pen) return
  paint(pen, width, height, readSlip(node))
  const link = document.createElement('a')
  link.href = canvas.toDataURL('image/png')
  link.download = `Zindigi-receipt-${safeName(receiptNo)}.png`
  link.click()
}

function readSlip(node: HTMLElement) {
  const values = [...node.querySelectorAll('dd')].map((item) => item.textContent || '')
  return {
    amount: node.querySelector('.slip-amount')?.textContent || '',
    rows: [
      ['Receipt no.', values[0] || ''],
      ['Application', values[1] || ''],
      ['Customer', values[2] || ''],
      ['Product', values[3] || ''],
      ['From wallet', values[4] || ''],
      ['To bank', values[5] || ''],
      ['Paid at', values[6] || ''],
    ],
  }
}

function paint(pen: CanvasRenderingContext2D, width: number, height: number, slip: { amount: string; rows: string[][] }) {
  pen.fillStyle = '#e7f0ef'
  pen.fillRect(0, 0, width, height)
  round(pen, 36, 36, width - 72, height - 72, 28)
  pen.fillStyle = '#ffffff'
  pen.fill()
  pen.save()
  pen.beginPath()
  pen.moveTo(36, 230)
  pen.lineTo(width - 36, 230)
  pen.lineTo(width - 36, 64)
  pen.quadraticCurveTo(width - 36, 36, width - 64, 36)
  pen.lineTo(64, 36)
  pen.quadraticCurveTo(36, 36, 36, 64)
  pen.closePath()
  pen.fillStyle = '#0c3d3a'
  pen.fill()
  pen.restore()
  pen.fillStyle = '#9ed4cf'
  pen.font = '700 18px Manrope, Segoe UI, sans-serif'
  pen.fillText('ZINDIGI', 72, 92)
  pen.fillStyle = '#ffffff'
  pen.font = '800 40px Manrope, Segoe UI, sans-serif'
  pen.fillText('Payment receipt', 72, 148)
  pen.font = '700 16px Manrope, Segoe UI, sans-serif'
  pen.fillText('PAID', 72, 186)
  pen.fillStyle = '#0c3d3a'
  pen.font = '800 48px Manrope, Segoe UI, sans-serif'
  pen.fillText(slip.amount, 72, 310)
  pen.fillStyle = '#5d6e7c'
  pen.font = '600 18px Manrope, Segoe UI, sans-serif'
  pen.fillText('Down payment received', 72, 348)
  let y = 410
  for (const [label, value] of slip.rows) {
    pen.strokeStyle = '#e3ecee'
    pen.beginPath()
    pen.moveTo(72, y - 28)
    pen.lineTo(width - 72, y - 28)
    pen.stroke()
    pen.fillStyle = '#5d6e7c'
    pen.font = '600 16px Manrope, Segoe UI, sans-serif'
    pen.fillText(label, 72, y)
    pen.fillStyle = '#172033'
    pen.font = '700 16px Manrope, Segoe UI, sans-serif'
    pen.textAlign = 'right'
    pen.fillText(fit(pen, value, width - 280), width - 72, y)
    pen.textAlign = 'left'
    y += 52
  }
  pen.fillStyle = '#5d6e7c'
  pen.font = '500 15px Manrope, Segoe UI, sans-serif'
  wrap(pen, 'This receipt is for the down payment only. The financed amount is not included.', 72, y + 10, width - 144, 22)
}

function downloadPdf(data: ReceiptData, when: string) {
  const lines = [
    ['Receipt no.', data.receiptNo],
    ['Application', data.applicationNo],
    ['Customer', data.customerName || '-'],
    ['Product', data.productTitle || '-'],
    ['From wallet', data.fromWallet || '-'],
    ['To bank', data.toWallet || '-'],
    ['Paid at', when],
  ]
  const amount = money(data.amount)
  const commands = [
    '0.047 0.239 0.227 rg',
    '0 470 420 170 re f',
    '0.620 0.831 0.812 rg',
    'BT /F1 11 Tf 36 590 Td (ZINDIGI) Tj ET',
    '1 1 1 rg',
    'BT /F1 22 Tf 36 555 Td (Payment receipt) Tj ET',
    'BT /F1 11 Tf 36 528 Td (PAID) Tj ET',
    '0.047 0.239 0.227 rg',
    `BT /F1 26 Tf 36 430 Td (${pdfText(amount)}) Tj ET`,
    '0.365 0.431 0.486 rg',
    'BT /F1 11 Tf 36 404 Td (Down payment received) Tj ET',
  ]
  let y = 360
  for (const [label, value] of lines) {
    commands.push('0.890 0.925 0.929 RG')
    commands.push(`36 ${y + 16} m 384 ${y + 16} l S`)
    commands.push('0.365 0.431 0.486 rg')
    commands.push(`BT /F1 10 Tf 36 ${y} Td (${pdfText(label)}) Tj ET`)
    commands.push('0.090 0.125 0.200 rg')
    commands.push(`BT /F1 10 Tf 180 ${y} Td (${pdfText(value)}) Tj ET`)
    y -= 28
  }
  commands.push('0.365 0.431 0.486 rg')
  commands.push(`BT /F1 9 Tf 36 ${y - 8} Td (Down payment only. The financed amount is not included.) Tj ET`)
  const stream = commands.join('\n')
  const pdf = buildPdf(stream)
  const blob = new Blob([pdf], { type: 'application/pdf' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `Zindigi-receipt-${safeName(data.receiptNo)}.pdf`
  link.click()
  URL.revokeObjectURL(link.href)
}

function buildPdf(stream: string) {
  const encoder = new TextEncoder()
  const objects = [
    '1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n',
    '2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n',
    '3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 420 640] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n',
    `4 0 obj << /Length ${encoder.encode(stream).length} >> stream\n${stream}\nendstream endobj\n`,
    '5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n',
  ]
  let body = '%PDF-1.4\n'
  const offsets = [0]
  for (const object of objects) {
    offsets.push(encoder.encode(body).length)
    body += object
  }
  const xrefAt = encoder.encode(body).length
  body += `xref\n0 ${objects.length + 1}\n`
  body += '0000000000 65535 f \n'
  for (let index = 1; index < offsets.length; index += 1) {
    body += `${String(offsets[index]).padStart(10, '0')} 00000 n \n`
  }
  body += `trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefAt}\n%%EOF`
  return encoder.encode(body)
}

function pdfText(value: string) {
  return value.replace(/[^\x20-\x7E]/g, '*').replace(/[\\()]/g, (char) => `\\${char}`)
}

function safeName(value: string) {
  return value.replace(/[^\w.-]+/g, '-')
}

function fit(pen: CanvasRenderingContext2D, value: string, max: number) {
  let text = value
  while (text.length > 3 && pen.measureText(text).width > max) text = `${text.slice(0, -2)}…`
  return text
}

function wrap(pen: CanvasRenderingContext2D, value: string, x: number, y: number, max: number, line: number) {
  const words = value.split(' ')
  let row = ''
  for (const word of words) {
    const next = row ? `${row} ${word}` : word
    if (pen.measureText(next).width > max) {
      pen.fillText(row, x, y)
      y += line
      row = word
    } else {
      row = next
    }
  }
  if (row) pen.fillText(row, x, y)
}

function round(pen: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  pen.beginPath()
  pen.moveTo(x + r, y)
  pen.arcTo(x + w, y, x + w, y + h, r)
  pen.arcTo(x + w, y + h, x, y + h, r)
  pen.arcTo(x, y + h, x, y, r)
  pen.arcTo(x, y, x + w, y, r)
  pen.closePath()
}
