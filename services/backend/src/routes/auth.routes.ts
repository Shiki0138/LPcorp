import { Router } from 'express';

const router = Router();

router.post('/login', async (req, res) => {
  res.json({ message: 'Login endpoint' });
});

router.post('/register', async (req, res) => {
  res.json({ message: 'Register endpoint' });
});

router.post('/magic-link', async (req, res) => {
  res.json({ message: 'Magic link endpoint' });
});

export default router;