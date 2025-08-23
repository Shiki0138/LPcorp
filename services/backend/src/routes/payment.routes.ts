import { Router } from 'express';

const router = Router();

router.post('/subscribe', async (req, res) => {
  res.json({ message: 'Subscribe to plan' });
});

router.post('/webhook', async (req, res) => {
  res.json({ message: 'Payment webhook' });
});

router.get('/plans', async (req, res) => {
  res.json({ 
    plans: [
      { id: 'starter', price: 9800 },
      { id: 'standard', price: 29800 },
      { id: 'professional', price: 49800 }
    ]
  });
});

export default router;