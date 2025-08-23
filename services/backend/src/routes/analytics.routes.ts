import { Router } from 'express';

const router = Router();

router.get('/heatmap/:lpId', async (req, res) => {
  res.json({ message: 'Get heatmap data' });
});

router.post('/track', async (req, res) => {
  res.json({ message: 'Track analytics event' });
});

router.get('/report/:lpId', async (req, res) => {
  res.json({ message: 'Get analytics report' });
});

export default router;