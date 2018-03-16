using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO.Ports;

namespace CS_Android_232
{
    public partial class Form1 : Form
    {
        public SerialPort _serialPort;
        public Form1()
        {
            InitializeComponent();
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            _serialPort = new SerialPort("COM8",9600, Parity.None, 8, StopBits.One);
            try
            {
                // Open serial port
                _serialPort.Open();
            }
            catch (Exception ex)
            {
                this.Text="Open() error: " + ex.Message;
            }
        }

        private void Form1_FormClosed(object sender, FormClosedEventArgs e)
        {
            _serialPort.Close();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            _serialPort.Write("A");
        }

        private void button2_Click(object sender, EventArgs e)
        {
            textBox1.Text =""+ _serialPort.ReadByte();
        }
    }
}
