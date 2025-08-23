import { Router } from 'express';

const router = Router();

router.get('/', async (req, res) => {
  res.json({ message: 'Get all LPs' });
});

router.post('/generate', async (req, res) => {
  res.json({ message: 'Generate LP endpoint' });
});

router.get('/:id', async (req, res) => {
  res.json({ message: 'Get LP by ID' });
});

export default router;