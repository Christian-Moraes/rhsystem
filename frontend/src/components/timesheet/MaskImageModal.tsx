import { useEffect, useRef, useState } from 'react'
import Modal from '../ui/Modal'
import Button from '../ui/Button'

interface Rect {
  x: number
  y: number
  w: number
  h: number
}

type Tool = 'crop' | 'mask'

interface MaskImageModalProps {
  file: File | null
  onClose: () => void
  onApply: (prepared: File) => void
}

/**
 * Prepara a foto antes do envio à IA.
 * Recortar: deixa só a folha no quadro (a leitura melhora quando a folha ocupa a imagem toda).
 * Tarja: pinta de preto nome, CTPS etc. (LGPD: minimização), então esses dados não saem do navegador.
 */
export default function MaskImageModal({ file, onClose, onApply }: MaskImageModalProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [image, setImage] = useState<HTMLImageElement | null>(null)
  const [tool, setTool] = useState<Tool>('crop')
  const [crop, setCrop] = useState<Rect | null>(null)
  const [masks, setMasks] = useState<Rect[]>([])
  const [drag, setDrag] = useState<Rect | null>(null)

  useEffect(() => {
    setMasks([])
    setCrop(null)
    setTool('crop')
    setImage(null)
    if (!file) return
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => setImage(img)
    img.src = url
    return () => URL.revokeObjectURL(url)
  }, [file])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !image) return
    canvas.width = image.naturalWidth
    canvas.height = image.naturalHeight
    const ctx = canvas.getContext('2d')!
    ctx.drawImage(image, 0, 0)
    ctx.fillStyle = '#000'
    const pendingMask = drag && tool === 'mask' ? [normalize(drag)] : []
    for (const r of [...masks, ...pendingMask]) ctx.fillRect(r.x, r.y, r.w, r.h)
    const c = drag && tool === 'crop' ? normalize(drag) : crop
    if (c) {
      // Escurece o que fica fora do recorte
      ctx.fillStyle = 'rgba(0,0,0,0.45)'
      ctx.fillRect(0, 0, canvas.width, c.y)
      ctx.fillRect(0, c.y + c.h, canvas.width, canvas.height - c.y - c.h)
      ctx.fillRect(0, c.y, c.x, c.h)
      ctx.fillRect(c.x + c.w, c.y, canvas.width - c.x - c.w, c.h)
      ctx.strokeStyle = '#2563eb'
      ctx.lineWidth = Math.max(2, canvas.width / 300)
      ctx.strokeRect(c.x, c.y, c.w, c.h)
    }
  }, [image, masks, crop, drag, tool])

  // Coordenadas do mouse convertidas para pixels da imagem original (o canvas é exibido reduzido)
  const toImage = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const box = e.currentTarget.getBoundingClientRect()
    return {
      x: ((e.clientX - box.left) / box.width) * e.currentTarget.width,
      y: ((e.clientY - box.top) / box.height) * e.currentTarget.height,
    }
  }

  const apply = () => {
    if (!image || !file) return
    const area = crop ?? { x: 0, y: 0, w: image.naturalWidth, h: image.naturalHeight }
    const out = document.createElement('canvas')
    out.width = Math.round(area.w)
    out.height = Math.round(area.h)
    const ctx = out.getContext('2d')!
    ctx.drawImage(image, -area.x, -area.y)
    ctx.fillStyle = '#000'
    for (const r of masks) ctx.fillRect(r.x - area.x, r.y - area.y, r.w, r.h)
    out.toBlob(
      (blob) => {
        if (!blob) return
        const name = file.name.replace(/\.[^.]+$/, '') + '.jpg'
        onApply(new File([blob], name, { type: 'image/jpeg' }))
      },
      'image/jpeg',
      0.92
    )
  }

  return (
    <Modal
      isOpen={!!file}
      onClose={onClose}
      title="Preparar imagem"
      description={
        tool === 'crop'
          ? 'Arraste para recortar só a folha de ponto: a leitura fica mais precisa quando a folha ocupa a imagem toda.'
          : 'Arraste sobre o nome do trabalhador, CTPS e outros dados pessoais. Deixe visíveis o mês/ano e a tabela de horários.'
      }
      size="xl"
      footer={
        <>
          <Button
            type="button"
            variant="outline"
            onClick={() => (tool === 'crop' ? setCrop(null) : setMasks((prev) => prev.slice(0, -1)))}
            disabled={tool === 'crop' ? !crop : !masks.length}
          >
            Desfazer
          </Button>
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="button" onClick={apply} disabled={!crop && !masks.length}>
            Aplicar
          </Button>
        </>
      }
    >
      <div className="flex gap-2 justify-center mb-3">
        <Button type="button" size="sm" variant={tool === 'crop' ? 'primary' : 'outline'} onClick={() => setTool('crop')}>
          Recortar folha {crop ? '✓' : ''}
        </Button>
        <Button type="button" size="sm" variant={tool === 'mask' ? 'primary' : 'outline'} onClick={() => setTool('mask')}>
          Ocultar dados pessoais {masks.length ? `(${masks.length})` : ''}
        </Button>
      </div>
      <canvas
        ref={canvasRef}
        className="max-h-[60vh] max-w-full mx-auto cursor-crosshair touch-none border border-gray-200"
        onPointerDown={(e) => {
          e.currentTarget.setPointerCapture(e.pointerId)
          const p = toImage(e)
          setDrag({ x: p.x, y: p.y, w: 0, h: 0 })
        }}
        onPointerMove={(e) => {
          if (!drag) return
          const p = toImage(e)
          setDrag({ ...drag, w: p.x - drag.x, h: p.y - drag.y })
        }}
        onPointerUp={() => {
          if (drag && Math.abs(drag.w) > 4 && Math.abs(drag.h) > 4) {
            if (tool === 'crop') setCrop(normalize(drag))
            else setMasks((prev) => [...prev, normalize(drag)])
          }
          setDrag(null)
        }}
      />
    </Modal>
  )
}

function normalize(r: Rect): Rect {
  return {
    x: Math.min(r.x, r.x + r.w),
    y: Math.min(r.y, r.y + r.h),
    w: Math.abs(r.w),
    h: Math.abs(r.h),
  }
}
